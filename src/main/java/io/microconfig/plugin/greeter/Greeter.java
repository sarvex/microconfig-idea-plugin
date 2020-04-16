package io.microconfig.plugin.greeter;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;

import java.awt.*;

import static com.intellij.notification.NotificationDisplayType.STICKY_BALLOON;
import static com.intellij.notification.NotificationListener.URL_OPENING_LISTENER;
import static com.intellij.notification.NotificationType.INFORMATION;
import static com.intellij.notification.impl.NotificationsManagerImpl.createBalloon;
import static com.intellij.ui.BalloonLayoutData.fullContent;

public class Greeter implements StartupActivity {
    private static final String displayId = "io.microconfig.plugin";
    private static final String message = "<br/>Thank you for using <b><a href=\"https://microconfig.io\">Microconfig.IO</a></b>!<br/>" +
            "If you have any questions or ideas you can join our <b><a href=\"https://join.slack.com/t/microconfig/shared_invite/zt-dflf2m0n-wOPVfAmk5eiHPn_9Omff7Q\">Slack</a></b>";

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void runActivity(Project project) {
        MicroconfigSettings settings = ServiceManager.getService(MicroconfigSettings.class);
        System.out.println("Settings version: " + settings.getVersion());
        IdeaPluginDescriptor descriptor = PluginManager.getPlugin(PluginId.getId("io.microconfig.idea-plugin"));

        if (descriptor != null && !descriptor.getVersion().equals(settings.getVersion())) {
            settings.setVersion(descriptor.getVersion());
            Notification notification = createNotification();
            IdeFrame frame = WindowManager.getInstance().getIdeFrame(project);
            if (frame == null) {
                notification.notify(project);
                return;
            }
            Rectangle bounds = frame.getComponent().getBounds();
            RelativePoint target = new RelativePoint(frame.getComponent(), new Point(bounds.x + bounds.width, 20));

            try {
                Balloon balloon = createBalloon(
                        frame,
                        notification,
                        true, // showCallout
                        false, // hideOnClickOutside
                        fullContent(),
                        project);
                balloon.show(target, Balloon.Position.atLeft);
            } catch (RuntimeException e) {
                notification.notify(project);
            }
        }
    }

    private Notification createNotification() {
        NotificationGroup group = new NotificationGroup(displayId, STICKY_BALLOON, true);
        return group.createNotification("Microconfig plugin updated", message, INFORMATION, URL_OPENING_LISTENER);
    }
}