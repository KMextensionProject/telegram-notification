package io.github.kmextensionproject.notification.telegram;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import io.github.kmextensionproject.notification.base.Message;
import io.github.kmextensionproject.notification.base.Notification;
import io.github.kmextensionproject.notification.base.NotificationResult;
import io.github.kmextensionproject.notification.base.NotificationResult.Status;
import io.github.kmextensionproject.notification.base.Recipient;
import io.github.kmextensionproject.notification.classloading.GlobalNotificationRegistry;
import io.github.kmextensionproject.notification.classloading.NotificationLoader;

class TelegramNotificationTest {

	@Test
	void loadTest() throws IOException {
		GlobalNotificationRegistry.getInstance().register(TelegramNotification.class);
		assertDoesNotThrow(() -> NotificationLoader.loadRegisteredNotifications());

		List<Notification> notifications = NotificationLoader.loadRegisteredNotifications();
		Notification notification = notifications.get(0);

		assertTrue(notifications.size() == 1);
		assertTrue(notification.getClass().equals(TelegramNotification.class));

		NotificationResult notifResult = notification.sendNotification(
				new Message("test message"),
				new Recipient().withOtherAddress("chatId"));

		assertEquals(Status.FAILURE, notifResult.getStatus());

	}
}
