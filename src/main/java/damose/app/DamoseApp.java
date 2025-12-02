package damose.app;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import damose.config.AppConstants;
import damose.controller.MainController;
import damose.database.DatabaseManager;
import damose.database.SessionManager;
import damose.model.ConnectionMode;
import damose.service.RealtimeService;
import damose.ui.dialog.LoadingDialog;
import damose.ui.dialog.LoginDialog;

/**
 * Main application entry point.
 */
public class DamoseApp {

    private static LoadingDialog loadingDialog;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Show login frame first
            LoginDialog loginDialog = new LoginDialog(null);
            
            // Set callback for when login completes
            loginDialog.setOnComplete(user -> {
                // Check if cancelled (user is null AND wasCancelled)
                if (loginDialog.wasCancelled()) {
                    System.out.println("Login cancelled, exiting...");
                    System.exit(0);
                    return;
                }

                // Set session (can be null if skipped)
                SessionManager.setCurrentUser(user);

                if (user != null) {
                    System.out.println("Logged in as: " + user.getUsername());
                } else {
                    System.out.println("Continuing without account");
                }

                // Start loading process
                startLoadingProcess();
            });
            
            // Show the login dialog
            loginDialog.setVisible(true);
        });

        // Shutdown hook to close database
        Runtime.getRuntime().addShutdownHook(new Thread(DatabaseManager::close));
    }

    private static void startLoadingProcess() {
        loadingDialog = new LoadingDialog(null);
        loadingDialog.setVisible(true);

        // Set callback for when loading is complete
        loadingDialog.setOnComplete(() -> {
            MainController controller = new MainController();
            controller.start();
        });

        // Run loading steps in background thread
        new Thread(() -> {
            try {
                // Step 1: Initialization
                SwingUtilities.invokeLater(() -> loadingDialog.stepInitStart());
                Thread.sleep(300);

                // Initialize database
                DatabaseManager.initialize();
                SwingUtilities.invokeLater(() -> loadingDialog.stepInitDone());
                Thread.sleep(200);

                // Step 2: Static GTFS data
                SwingUtilities.invokeLater(() -> loadingDialog.stepStaticStart());

                SwingUtilities.invokeLater(() -> loadingDialog.stepStaticProgress("stops.txt"));
                Thread.sleep(400);
                SwingUtilities.invokeLater(() -> loadingDialog.stepStaticProgress("trips.txt"));
                Thread.sleep(400);
                SwingUtilities.invokeLater(() -> loadingDialog.stepStaticProgress("stop_times.txt"));
                Thread.sleep(500);
                SwingUtilities.invokeLater(() -> loadingDialog.stepStaticProgress("calendar_dates.txt"));
                Thread.sleep(300);

                SwingUtilities.invokeLater(() -> loadingDialog.stepStaticDone(8000, 15000));
                Thread.sleep(300);

                // Step 3: RT data
                SwingUtilities.invokeLater(() -> loadingDialog.stepRTStart(AppConstants.RT_TIMEOUT_SECONDS));

                // Set up callback for when RT data is received
                RealtimeService.setOnDataReceived(() -> {
                    SwingUtilities.invokeLater(() -> {
                        loadingDialog.stepRTDone();
                        finishLoading();
                    });
                });

                // Start fetching RT data
                RealtimeService.setMode(ConnectionMode.ONLINE);
                RealtimeService.fetchRealtimeFeeds();

                // Timeout check
                Timer timeoutCheck = new Timer(AppConstants.RT_TIMEOUT_SECONDS * 1000 + 500, e -> {
                    ((Timer) e.getSource()).stop();
                    if (!loadingDialog.isDataReceived()) {
                        SwingUtilities.invokeLater(() -> finishLoading());
                    }
                });
                timeoutCheck.setRepeats(false);
                timeoutCheck.start();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "LoadingThread").start();
    }

    private static void finishLoading() {
        loadingDialog.stepAppStart();

        Timer finishTimer = new Timer(400, e -> {
            ((Timer) e.getSource()).stop();
            loadingDialog.completeAndClose();
        });
        finishTimer.setRepeats(false);
        finishTimer.start();
    }
}
