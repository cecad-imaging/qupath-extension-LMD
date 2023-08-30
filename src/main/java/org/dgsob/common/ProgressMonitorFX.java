package org.dgsob.common;

import javafx.animation.Timeline;
import javafx.scene.Node;
import qupath.lib.plugins.SimpleProgressMonitor;
import qupath.lib.regions.ImageRegion;

import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.animation.KeyFrame;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import javafx.scene.control.Control;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.RowConstraints;

public class ProgressMonitorFX implements SimpleProgressMonitor {
    private static final Logger logger = LoggerFactory.getLogger(ProgressMonitorFX.class);

    private final Stage owner;

    private Timeline timeline;

    private Dialog<Void> progressDialog;
    private Label progressLabel;
    private String lastMessage;
    private final AtomicInteger progress = new AtomicInteger(0);
    private int maxProgress;
    private final int millisToDisplay;
    private boolean taskComplete = false;
    private long startTimeMS = 0;

    public ProgressMonitorFX(final Stage owner) {
        this(owner, 500);
    }

    public ProgressMonitorFX(final Stage owner, int millisToDisplay) {
        this.owner = owner;
        this.millisToDisplay = millisToDisplay;
    }

    @Override
    public void startMonitoring(final String message, final int maxProgress, final boolean mayCancel) {
        if (progressDialog != null) {
            throw new UnsupportedOperationException("Unsupported attempt to reuse a plugin progress monitor!");
        }

        this.startTimeMS = System.currentTimeMillis();
        this.maxProgress = maxProgress;
        this.progress.set(0);

        createProgressDialog(message);
    }

    void createProgressDialog(final String message) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> createProgressDialog(message));
            return;
        }

        taskComplete = false;

        progressLabel = new Label("");
        progressDialog = new Dialog<>();

        progressDialog.initOwner(owner);
        String STARTING_MESSAGE = "Starting...";
        progressDialog.getDialogPane().setHeaderText(message == null ? STARTING_MESSAGE : message);

        BorderPane pane = new BorderPane();
        progressDialog.setTitle("Progress");

        Node panel = createRowGridControls(progressLabel);
        pane.setCenter(panel);

        progressDialog.initModality(Modality.APPLICATION_MODAL);
        pane.setPadding(new Insets(10, 10, 10, 10));
        progressDialog.getDialogPane().setContent(pane);

        // Show dialog after a delay
        Duration duration = millisToDisplay > 0 ? Duration.millis(millisToDisplay) : Duration.millis(500);
        if (timeline == null) {
            timeline = new Timeline(new KeyFrame(
                    duration,
                    e -> updateDialog()));
        }
        if (millisToDisplay > 0) {
            timeline.setDelay(duration);
        }
        timeline.setCycleCount(Timeline.INDEFINITE);
        if (!taskComplete) {
            timeline.playFromStart();
        }
    }

    @Override
    public boolean cancelled() {
        return false;
    }

    @Override
    public void updateProgress(final int progressIncrement, final String message, final ImageRegion region) {
        progress.addAndGet(progressIncrement);
        this.lastMessage = message;
    }

    @Override
    public void pluginCompleted(final String message) {
        stopMonitoring(message);
    }

    void stopMonitoring(final String message) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> stopMonitoring(message));
            return;
        }

        if (timeline != null) {
            timeline.stop();
        }

        if (taskComplete && (progressDialog == null || !progressDialog.isShowing())) {
            return;
        }

        taskComplete = true;
        if (progressDialog != null) {
            doDialogClose();
        }
        long endTime = System.currentTimeMillis();
        logger.info(String.format("Processing complete in %.2f seconds", (endTime - startTimeMS)/1000.));
        if (message != null && !message.trim().isEmpty()) {
            logger.info(message);
        }
    }

    private void updateDialog() {
        if (!progressDialog.isShowing() && !taskComplete) {
            progressDialog.show();
        }

        int progressValue = progress.get();
        int progressPercent = (int)Math.round((double)progressValue / maxProgress * 100.0);

        if (!taskComplete) {
            String RUNNING_MESSAGE = "Running...";
            progressDialog.getDialogPane().setHeaderText(RUNNING_MESSAGE);
        }

        if (lastMessage == null) {
            progressLabel.setText("");
        } else {
            progressLabel.setText(lastMessage + " (" + progressPercent + "%)");
        }
        if (progressValue >= maxProgress) {
            String COMPLETED_MESSAGE = "Completed!";
            stopMonitoring(COMPLETED_MESSAGE);
        }
    }

    private void doDialogClose() {
        if (progressDialog != null) {
            progressDialog.close();
        }
        if (timeline != null) {
            timeline.stop();
        }
    }

    public static GridPane createRowGridControls(final Node... nodes) {
        GridPane pane = new GridPane();
        int n = nodes.length;
        for (int i = 0; i < n; i++) {
            RowConstraints row = new RowConstraints();
            row.setPercentHeight(100.0/n);
            pane.getRowConstraints().add(row);
            Node node = nodes[i];
            pane.add(node, 0, i);
            if (node instanceof Control) {
                ((Control)node).prefWidthProperty().bind(pane.widthProperty());
            }
        }
        ColumnConstraints col = new ColumnConstraints();
        col.setPercentWidth(100);
        pane.getColumnConstraints().add(col);
        return pane;
    }
}
