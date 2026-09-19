package com.minera.mvp.desktop;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class DesktopApp extends Application {

    private static final String API = "http://localhost:8080/api";

    private final ApiClient api = new ApiClient(API);
    private Stage stage;
    private Map<String, Object> user;
    private String lastRunId = "";
    private final List<Button> navButtons = new ArrayList<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("MineOps - Sistema de escritorio");
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        showLogin();
        stage.show();
    }

    // ===================== LOGIN =====================

    private void showLogin() {
        Label title = label("MineOps", "-fx-font-size:26px; -fx-font-weight:bold; -fx-text-fill:#1e293b;");
        Label subtitle = label("Cliente de escritorio de operaciones mineras",
                "-fx-text-fill:#64748b;");

        TextField email = new TextField();
        email.setPromptText("Email");
        email.setText("admin@mineraandina.com");
        styleInput(email);

        PasswordField password = new PasswordField();
        password.setPromptText("Contraseña");
        password.setText("admin123");
        styleInput(password);

        Label error = label("", "-fx-text-fill:#dc2626;");
        Label ok = label("", "-fx-text-fill:#16a34a;");

        Button btn = new Button("Ingresar");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle(primaryStyle());

        Button checkServer = new Button("Comprobar servidor");
        checkServer.setStyle("-fx-background-color:transparent; -fx-text-fill:#2563eb; -fx-cursor:hand;");

        btn.setOnAction(e -> {
            btn.setDisable(true);
            error.setText("");
            ok.setText("");
            new Thread(() -> {
                try {
                    Map<String, Object> res = api.login(email.getText().trim(), password.getText());
                    api.setToken(String.valueOf(res.get("token")));
                    Map<String, Object> u = castMap(res.get("user"));
                    Platform.runLater(() -> {
                        user = u;
                        showMain();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        error.setText(ex.getMessage());
                        btn.setDisable(false);
                    });
                }
            }).start();
        });

        checkServer.setOnAction(e -> new Thread(() -> {
            String text;
            try {
                java.net.http.HttpClient c = java.net.http.HttpClient.newHttpClient();
                var req = java.net.http.HttpRequest.newBuilder(
                                java.net.URI.create(API + "/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{}"))
                        .build();
                var res = c.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                text = res.statusCode() == 401 || res.statusCode() == 400
                        ? "Servidor ONLINE (puerto 8080)" : "Respuesta inesperada: HTTP " + res.statusCode();
            } catch (Exception ex) {
                text = "Servidor OFFLINE. Ejecuta 'run-backend.bat' primero.";
            }
            final String out = text;
            Platform.runLater(() -> ok.setText(out));
        }).start());

        VBox box = new VBox(12, title, subtitle, email, password, error, btn, checkServer, ok);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new javafx.geometry.Insets(40));
        box.setMaxWidth(380);
        box.setStyle("-fx-background-color:white; -fx-background-radius:16; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 24, 0.2, 0, 6);");

        Region spacer1 = new Region();
        Region spacer2 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        HBox wrap = new HBox(spacer1, box, spacer2);
        VBox.setVgrow(wrap, Priority.ALWAYS);
        VBox root = new VBox(wrap);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #0f172a, #26355c);");
        stage.setScene(new Scene(root, 1000, 650));
        email.setOnAction(e -> password.requestFocus());
        password.setOnAction(e -> btn.fire());
    }

    // ===================== PRINCIPAL =====================

    private void showMain() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#f1f5f9;");

        VBox sidebar = new VBox(6);
        sidebar.setPadding(new javafx.geometry.Insets(16));
        sidebar.setStyle("-fx-background-color:#0f172a;");
        sidebar.setPrefWidth(230);

        Label brand = label("⛏  MineOps", "-fx-text-fill:white; -fx-font-size:19px; -fx-font-weight:bold;");
        Label company = label("Empresa: " + str(user, "companyName"),
                "-fx-text-fill:#94a3b8; -fx-font-size:11px; -fx-wrap-text:true;");
        Label userLine = label(str(user, "fullName") + " · " + str(user, "role"),
                "-fx-text-fill:#cbd5e1; -fx-font-size:11px;");

        Button dashBtn = navButton("Dashboard");
        Button uploadBtn = navButton("Cargar archivo");
        Button histBtn = navButton("Historial");
        navButtons.clear();
        navButtons.add(dashBtn);
        navButtons.add(uploadBtn);
        navButtons.add(histBtn);

        Button logoutBtn = new Button("Cerrar sesión");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setStyle("-fx-background-color:#334155; -fx-text-fill:white; -fx-cursor:hand; "
                + "-fx-background-radius:8; -fx-padding:10;");

        VBox content = new VBox();
        content.setPadding(new javafx.geometry.Insets(24));
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;");

        dashBtn.setOnAction(e -> { activateNav(dashBtn); showDashboard(content); });
        uploadBtn.setOnAction(e -> { activateNav(uploadBtn); showUpload(content); });
        histBtn.setOnAction(e -> { activateNav(histBtn); showHistory(content); });

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().addAll(brand, company, userLine, new Separator(),
                dashBtn, uploadBtn, histBtn, spacer, logoutBtn);

        logoutBtn.setOnAction(e -> { user = null; api.setToken(""); showLogin(); });

        root.setLeft(sidebar);
        root.setCenter(scroll);
        stage.setScene(new Scene(root, 1100, 700));

        activateNav(dashBtn);
        showDashboard(content);
    }

    private void activateNav(Button activeBtn) {
        for (Button b : navButtons) {
            b.setStyle(b == activeBtn ? activeStyle() : idleStyle());
        }
    }

    private String idleStyle() {
        return "-fx-background-color:transparent; -fx-text-fill:#cbd5e1; -fx-font-size:14px; "
                + "-fx-alignment:CENTER-LEFT; -fx-padding:10 14; -fx-background-radius:8; -fx-cursor:hand;";
    }

    private String activeStyle() {
        return "-fx-background-color:#f59e0b; -fx-text-fill:#1e293b; -fx-font-weight:bold; "
                + "-fx-font-size:14px; -fx-alignment:CENTER-LEFT; -fx-padding:10 14; -fx-background-radius:8;";
    }

    private Button navButton(String text) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setStyle(idleStyle());
        return b;
    }

    // ===================== DASHBOARD =====================

    private void showDashboard(VBox content) {
        loading(content);
        new Thread(() -> {
            try {
                Map<String, Object> d = api.get("/dashboard");
                Platform.runLater(() -> {
                    content.getChildren().clear();
                    content.getChildren().add(title("Dashboard de operación",
                            "Resumen de los procesos de " + str(user, "companyName")));

                    HBox cards = new HBox(12);
                    cards.getChildren().add(kpi("Procesos", str(d, "totalRuns")));
                    cards.getChildren().add(kpi("Filas válidas", str(d, "validRows")));
                    cards.getChildren().add(kpi("Filas inválidas", str(d, "invalidRows")));
                    cards.getChildren().add(kpi("Tonelaje (t)", str(d, "totalTonnage")));
                    cards.getChildren().add(kpi("Ley media Cu", str(d, "avgGrade")));
                    content.getChildren().add(cards);

                    content.getChildren().add(card("Tonelaje por zona", zonesText(d)));
                    content.getChildren().add(card("Filas por estado", estadosText(d)));
                    content.getChildren().add(card("Últimas ejecuciones", recentRunsText(d)));
                });
            } catch (Exception ex) {
                Platform.runLater(() -> error(content, "No se pudo cargar el dashboard: " + ex.getMessage()));
            }
        }).start();
    }

    private String zonesText(Map<String, Object> d) {
        StringBuilder sb = new StringBuilder();
        for (Object o : list(d.get("perZone"))) {
            Map<String, Object> z = castMap(o);
            sb.append("• ").append(str(z, "name")).append(": ").append(dnum(z, "value")).append(" t\n");
        }
        return sb.length() == 0 ? "Sin datos todavía. Carga tu primer archivo." : sb.toString();
    }

    private String estadosText(Map<String, Object> d) {
        StringBuilder sb = new StringBuilder();
        for (Object o : list(d.get("perEstado"))) {
            Map<String, Object> z = castMap(o);
            sb.append("• ").append(str(z, "name")).append(": ").append(lnum(z, "value")).append(" filas\n");
        }
        return sb.length() == 0 ? "Sin datos." : sb.toString();
    }

    private String recentRunsText(Map<String, Object> d) {
        StringBuilder sb = new StringBuilder();
        for (Object o : list(d.get("recentRuns"))) {
            Map<String, Object> r = castMap(o);
            sb.append("[").append(str(r, "status")).append("] ").append(str(r, "originalName"))
                    .append(" → ").append(lnum(r, "validRows")).append(" válidas / ")
                    .append(lnum(r, "invalidRows")).append(" inválidas · ")
                    .append(str(r, "totalTonnage")).append(" t · ")
                    .append(str(r, "executedByName")).append("\n");
        }
        return sb.length() == 0 ? "Sin procesos todavía." : sb.toString();
    }

    // ===================== SUBIR ARCHIVO =====================

    private void showUpload(VBox content) {
        content.getChildren().clear();
        content.getChildren().add(title("Cargar datos de operación",
                "Sube un CSV o Excel. El sistema valida, transforma y cruza con el catálogo."));

        VBox box = new VBox(14);
        box.setStyle(cardStyle());
        Label fileLbl = label("Ningún archivo seleccionado", "-fx-text-fill:#64748b;");
        AtomicReference<Path> chosen = new AtomicReference<>();

        Button runBtn = new Button("Subir y procesar");
        runBtn.setStyle(primaryStyle());
        runBtn.setDisable(true);
        Button reportBtn = new Button("Descargar Excel y PDF");
        reportBtn.setStyle(secondaryStyle());
        reportBtn.setDisable(true);

        Button select = new Button("Elegir archivo (.csv / .xlsx)");
        select.setStyle(primaryStyle());
        select.setOnAction(e -> {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("Excel y CSV", "*.csv", "*.xlsx", "*.xls"));
            File f = fc.showOpenDialog(stage);
            if (f != null) {
                chosen.set(f.toPath());
                runBtn.setDisable(false);
                fileLbl.setText("✓ " + f.getName());
            }
        });

        Label msg = label("", "-fx-text-fill:#16a34a; -fx-wrap-text:true;");
        Label err = label("", "-fx-text-fill:#dc2626; -fx-wrap-text:true;");
        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(22, 22);
        progress.setVisible(false);

        runBtn.setOnAction(e -> {
            Path file = chosen.get();
            if (file == null) return;
            runBtn.setDisable(true);
            progress.setVisible(true);
            msg.setText("");
            err.setText("");
            new Thread(() -> {
                try {
                    Map<String, Object> up = api.uploadFile("/uploads", file);
                    Map<String, Object> run = api.postJson("/uploads/" + up.get("id") + "/run", Map.of());
                    Platform.runLater(() -> {
                        progress.setVisible(false);
                        runBtn.setDisable(false);
                        lastRunId = String.valueOf(run.get("id"));
                        if ("ERROR".equals(str(run, "status"))) {
                            err.setText(logs(run));
                        } else {
                            msg.setText("Proceso OK → " + str(run, "validRows") + " válidas, "
                                    + str(run, "invalidRows") + " inválidas · "
                                    + str(run, "totalTonnage") + " t · ley "
                                    + str(run, "avgGrade") + " %");
                            reportBtn.setDisable(false);
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        progress.setVisible(false);
                        runBtn.setDisable(false);
                        err.setText(ex.getMessage());
                    });
                }
            }).start();
        });

        reportBtn.setOnAction(e -> new Thread(() -> {
            try {
                Path home = Path.of(System.getProperty("user.home"), "Downloads", "MineOps_reportes");
                Files.createDirectories(home);
                api.download("/runs/" + lastRunId + "/download/excel", home.resolve("reporte.xlsx"));
                api.download("/runs/" + lastRunId + "/download/pdf", home.resolve("reporte.pdf"));
                Desktop.getDesktop().open(home.toFile());
            } catch (Exception ex) {
                Platform.runLater(() -> err.setText("No se pudo descargar: " + ex.getMessage()));
            }
        }).start());

        HBox buttons = new HBox(10, select, runBtn, reportBtn);
        buttons.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().addAll(fileLbl, buttons, progress, msg, err);
        content.getChildren().add(box);

        TextArea format = new TextArea(formatHelp());
        format.setEditable(false);
        format.setWrapText(true);
        format.setPrefHeight(170);
        content.getChildren().add(cardOnly("Formato esperado", format));
    }

    private String formatHelp() {
        return "Columnas esperadas:\n  fecha; turno; zona; equipo; tonelaje; ley_cu; estado\n\n"
                + "Reglas que aplica el sistema:\n"
                + "  • fecha: 12/09/2026 o 2026-09-12\n"
                + "  • turno: A, B o C\n"
                + "  • tonelaje: mayor a 0 y menor a 100000 (acepta coma o punto decimal)\n"
                + "  • ley_cu: entre 0 y 100\n"
                + "  • equipo: debe existir en el catálogo de tu empresa\n\n"
                + "En la carpeta sample-data/ tenés archivos de ejemplo para probar.";
    }

    // ===================== HISTORIAL =====================

    private void showHistory(VBox content) {
        loading(content);
        new Thread(() -> {
            try {
                List<Object> runs = api.getList("/runs");
                Platform.runLater(() -> {
                    content.getChildren().clear();
                    content.getChildren().add(title("Historial de ejecuciones",
                            "Cada proceso guarda resultados, KPIs, log y reportes."));
                    if (runs.isEmpty()) {
                        content.getChildren().add(card("Historial", "Sin procesos todavía."));
                        return;
                    }
                    StringBuilder sb = new StringBuilder();
                    for (Object o : runs) {
                        Map<String, Object> r = castMap(o);
                        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
                                .append("#").append(str(r, "id")).append("  [").append(str(r, "status")).append("]\n")
                                .append("Archivo: ").append(str(r, "originalName")).append("\n")
                                .append("Fecha: ").append(str(r, "finishedAt")).append("\n")
                                .append("Válidas/Inválidas: ").append(str(r, "validRows")).append(" / ")
                                .append(str(r, "invalidRows")).append("\n")
                                .append("Tonelaje: ").append(str(r, "totalTonnage")).append(" t · Ley: ")
                                .append(str(r, "avgGrade")).append(" %\n")
                                .append("Duración: ").append(dnum(r, "durationMs") / 1000.0).append(" s · Usuario: ")
                                .append(str(r, "executedByName")).append("\n")
                                .append("Reportes: ").append(str(r, "excelFileName")).append(" / ")
                                .append(str(r, "pdfFileName")).append("\n")
                                .append("Log:\n").append(logs(r)).append("\n");
                    }
                    TextArea area = new TextArea(sb.toString());
                    area.setEditable(false);
                    area.setPrefHeight(460);
                    content.getChildren().add(cardOnly("Detalle", area));
                });
            } catch (Exception ex) {
                Platform.runLater(() -> error(content, "No se pudo cargar el historial: " + ex.getMessage()));
            }
        }).start();
    }

    // ===================== HELPERS UI =====================

    private VBox title(String t, String sub) {
        VBox v = new VBox(4);
        v.getChildren().add(label(t, "-fx-font-size:22px; -fx-font-weight:bold; -fx-text-fill:#0f172a;"));
        v.getChildren().add(label(sub, "-fx-text-fill:#64748b;"));
        VBox.setMargin(v, new javafx.geometry.Insets(0, 0, 10, 0));
        return v;
    }

    private VBox card(String title, String text) {
        VBox v = new VBox(8);
        v.setStyle(cardStyle());
        v.getChildren().add(label(title, "-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#0f172a;"));
        v.getChildren().add(label(text, "-fx-text-fill:#475569; -fx-wrap-text:true; -fx-font-size:13px;"));
        return v;
    }

    private VBox cardOnly(String title, Region body) {
        VBox v = new VBox(8);
        v.setStyle(cardStyle());
        v.getChildren().add(label(title, "-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#0f172a;"));
        v.getChildren().add(body);
        return v;
    }

    private VBox kpi(String labelText, String value) {
        VBox v = new VBox(6);
        v.setStyle(cardStyle());
        v.setPrefWidth(160);
        v.getChildren().add(label(value, "-fx-font-size:24px; -fx-font-weight:bold; -fx-text-fill:#f59e0b;"));
        v.getChildren().add(label(labelText, "-fx-text-fill:#64748b; -fx-font-size:12px;"));
        return v;
    }

    private void loading(VBox content) {
        content.getChildren().clear();
        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(40, 40);
        VBox v = new VBox(10, pi, label("Cargando...", "-fx-text-fill:#64748b;"));
        v.setAlignment(Pos.CENTER);
        content.getChildren().add(v);
    }

    private void error(VBox content, String message) {
        content.getChildren().clear();
        Label l = label(message, "-fx-text-fill:#dc2626; -fx-wrap-text:true;");
        content.getChildren().add(l);
    }

    private Label label(String text, String style) {
        Label l = new Label(text);
        l.setStyle(style);
        return l;
    }

    private String cardStyle() {
        return "-fx-background-color:white; -fx-background-radius:12; -fx-padding:16; "
                + "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.06), 10, 0.1, 0, 3);";
    }

    private void styleInput(TextField f) {
        f.setStyle("-fx-background-radius:8; -fx-border-radius:8; "
                + "-fx-border-color:#cbd5e1; -fx-padding:10 12; -fx-font-size:14px;");
        f.setMaxWidth(Double.MAX_VALUE);
    }

    private String primaryStyle() {
        return "-fx-background-color:#f59e0b; -fx-text-fill:#1e293b; -fx-font-weight:bold; "
                + "-fx-background-radius:8; -fx-padding:11 16 11 16; -fx-cursor:hand;";
    }

    private String secondaryStyle() {
        return "-fx-background-color:#059669; -fx-text-fill:white; -fx-font-weight:bold; "
                + "-fx-background-radius:8; -fx-padding:11 16 11 16; -fx-cursor:hand;";
    }

    // ===================== HELPERS DATOS =====================

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object o) {
        return (Map<String, Object>) o;
    }

    @SuppressWarnings("unchecked")
    private List<Object> list(Object o) {
        return o instanceof List<?> l ? (List<Object>) l : List.of();
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? "—" : String.valueOf(v);
    }

    private double dnum(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof Number n ? n.doubleValue() : 0;
    }

    private long lnum(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof Number n ? n.longValue() : 0;
    }

    private String logs(Map<String, Object> r) {
        String l = str(r, "logs");
        return l == null || l.equals("—") ? "" : l;
    }
}