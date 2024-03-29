package io.github.kmextensionproject.notification.telegram;

import static io.github.kmextensionproject.notification.base.NotificationResult.failure;
import static io.github.kmextensionproject.notification.base.NotificationResult.success;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.lineSeparator;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.logging.Level.FINE;
import static java.util.logging.Logger.getLogger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Logger;

import io.github.kmextensionproject.notification.base.Message;
import io.github.kmextensionproject.notification.base.Notification;
import io.github.kmextensionproject.notification.base.NotificationResult;
import io.github.kmextensionproject.notification.base.Recipient;

/**
 * This class uses Telegram chat bot to send simple text messages to chat group.
 * <p>
 * To use this class properly, user must do the following:
 * <ul>
 * 	<li>export ${telegram_bot_id} as environment variable</li>
 * 	<li>set ${chat_id} via {@link Recipient#withOtherAddress(String)}</li>
 * </ul>
 *
 * @author mkrajcovic
 */
public class TelegramNotification implements Notification {

	private static final Logger LOG = getLogger(TelegramNotification.class.getName());

	private final String BOT_ID = loadBotId();
	private final String SEND_MESSAGE_URL = "https://api.telegram.org/bot" + BOT_ID + "/sendMessage?chat_id=%1$s&text=%2$s&parse_mode=html";

	// based on proper configuration
	private boolean sendingEnabled = true;

	/**
	 * The subject of the provided message will be automatically formated as bold
	 * and the message body will be placed on the next line. If different approach
	 * is desired, then use only message body.
	 * <p>
	 * <b>Note:</b> This implementation supports only HTML parse mode, the usage is
	 * described here:
	 * <a href="https://core.telegram.org/bots/api#formatting-options">telegram
	 * formatting options</a>
	 *
	 * Does not even try to send the notification message if message is {@code null}
	 * or empty or recipient's {@code otherAddress} is {@code null} or empty
	 */
	@Override
	public NotificationResult sendNotification(Message message, Recipient recipient) {
		requireNonNull(message, "message cannot be null");
		requireNonNull(recipient, "recipient cannot be null");

		if (!sendingEnabled) {
			return failure("Can not send Telegram message - missing proper configuration, check logs for missing variables");
		} else if ((isBlank(message.getSubject()) && isBlank(message.getBody())) || isBlank(recipient.getOtherAddress())) {
			return failure("Can not send Telegram message - none of message subject or body is present or recipient's chat id is missing");
		}

		String chatId = recipient.getOtherAddress();
		return sendTelegramMessage(BOT_ID, chatId, formatMessage(message));
	}

	private boolean isBlank(String value) {
		return isNull(value) || value.trim().isEmpty();
	}

	private String formatMessage(Message message) {
		String formattedMessage = "";
		String subject = message.getSubject();
		String body = message.getBody();
		boolean hasSubject = subject != null && !subject.trim().isEmpty();
		if (hasSubject) {
			formattedMessage = "<b>" + subject + "</b>";
		}
		if (body != null) {
			if (!body.trim().isEmpty() && hasSubject) {
				formattedMessage += lineSeparator();
			}
			formattedMessage += body;
		}
		return formattedMessage.trim();
	}

	private NotificationResult sendTelegramMessage(String botId, String chatId, String message) {
		String finalUrl = format(SEND_MESSAGE_URL, chatId, encodeUrlMessage(message));
		// This can be extracted as a common HTTP sending facility
		// Note: if usage is intended to be this simple, do not delegate to any HttpClient API
		long callStart = currentTimeMillis();
		try {
			HttpURLConnection httpRequest = (HttpURLConnection) new URL(finalUrl).openConnection();
			int responseCode = httpRequest.getResponseCode();
			String responseMessage = httpRequest.getResponseMessage();
			httpRequest.disconnect();
			if (responseCode != 200) {
				return failure("Unsuccessful attempt to send Telegram notification the server returned: " + responseMessage);
			}
		} catch (IOException ioex) {
			return failure("Unable to send Telegram notification", ioex);
		} finally {
			if (LOG.isLoggable(FINE)) {
				LOG.fine("Calling " + finalUrl + (currentTimeMillis() - callStart) + "ms");
			}
		}
		return success("Telegram message sent successfully");
	}

	private String encodeUrlMessage(String message) {
		try {
			return URLEncoder.encode(message, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("unable to URL encode message with UTF-8 encoding");
		}
	}

	private String loadBotId() {
		String botId = System.getenv("telegram_bot_id");
		if (isBlank(botId)) {
			LOG.warning("${telegram_bot_id} environment varible must be set to use telegram notifications");
			disableSending();
		}
		return botId;
	}

	private void disableSending() {
		sendingEnabled = false;
	}
}
