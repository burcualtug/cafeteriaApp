package datamodels

data class PushNotification(
    val data: NotificationData,
    val to: String
)