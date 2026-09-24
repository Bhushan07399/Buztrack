export interface PushNotificationPayload {
  userId: string;
  title: string;
  body: string;
  data?: Record<string, string>;
}

export interface PushNotificationService {
  sendPushNotification(payload: PushNotificationPayload): Promise<boolean>;
}

export class MockPushService implements PushNotificationService {
  async sendPushNotification(payload: PushNotificationPayload): Promise<boolean> {
    console.log(`[Push Notification] To User ${payload.userId}: ${payload.title} - ${payload.body}`);
    return true;
  }
}
