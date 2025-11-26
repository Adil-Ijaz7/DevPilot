import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import com.formdev.flatlaf.FlatLightLaf; // Use FlatLaf core Light theme

/**
 * AI Pair Programmer (Polished UI Version with FlatLaf and History)
 * Added DSA-based history storage using ArrayList and a simple UI button to view history.
 * MODIFIED: Forced vertical wrapping in the output pane to eliminate horizontal scrollbar.
 */
public class PairProgrammer extends JFrame {

    private final JTextArea inputCodeArea;
    private final JEditorPane outputResultArea;
    private final JComboBox<String> analysisTypeComboBox;
    private final JComboBox<String> modelComboBox;
    private final JButton analyzeButton;
    private final JButton copyButton;
    private final JButton historyButton; // New button for history
    private final JProgressBar progressBar;

    // History storage: Using ArrayList (DSA) for in-memory storage of chat history
    private static final List<HistoryEntry> history = new ArrayList<>();

    // API Key (read from environment variable to avoid hardcoding secrets)
    private static final String API_KEY = System.getenv("OPENROUTER_API_KEY");
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";

    // UI Color and Font Constants (Adjusted for FlatLaf compatibility)
    private static final Color BG_COLOR = new Color(255, 255, 255); // Matches FlatLaf light theme
    private static final Color COMPONENT_BG_COLOR = new Color(255, 255, 255);
    private static final Color TEXT_AREA_BG_COLOR = new Color(255, 255, 255);
    private static final Color FOREGROUND_COLOR = new Color(0, 0, 0);
    private static final Color BORDER_COLOR = new Color(80, 80, 80);
    private static final Color ACCENT_COLOR_BLUE = new Color(0, 122, 204);
    private static final Font UI_FONT = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font CODE_FONT = new Font("Consolas", Font.PLAIN, 14);

    public PairProgrammer() {
        // Frame Setup
        setTitle("DevPilot");
        setSize(1280, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_COLOR);

        // Header Panel (Top Bar with Logo + Title)
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        headerPanel.setBackground(COMPONENT_BG_COLOR);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));

        // Logo
        JLabel logoLabel = new JLabel();
        ImageIcon logoIcon = new ImageIcon("logo.png"); // Place logo.png in project root
        Image scaledLogo = logoIcon.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
        logoLabel.setIcon(new ImageIcon(scaledLogo));
        headerPanel.add(logoLabel);

        // Title
        JLabel titleLabel = new JLabel("DevPilot");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(FOREGROUND_COLOR);
        headerPanel.add(titleLabel);

        add(headerPanel, BorderLayout.NORTH);

        // Main Content Panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(BG_COLOR);

        // Input and Output Panels
        JPanel inputPanel = createTextPanel("Your Code", inputCodeArea = new JTextArea());
        inputCodeArea.setText(getSampleCode());

        // --- MODIFICATION START ---
        // Use an anonymous inner class for JEditorPane to override its scrolling behavior.
        outputResultArea = new JEditorPane() {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                // This forces the content to wrap vertically and disables horizontal scrolling.
                return true;
            }
        };
        // --- MODIFICATION END ---

        JPanel outputPanel = createTextPanel("AI Feedback", outputResultArea);
        outputResultArea.setContentType("text/html");
        outputResultArea.setEditable(false);
        outputResultArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        // Split Pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputPanel, outputPanel);
        splitPane.setResizeWeight(0.5);
        splitPane.setBorder(null);
        splitPane.setDividerSize(8);

        // Controls Panel
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        controlsPanel.setBackground(COMPONENT_BG_COLOR);
        controlsPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR));

        controlsPanel.add(createStyledLabel("Analysis Type:"));
        String[] analysisTypes = {"Explain Code", "Find Bugs", "Refactor Code", "Add Comments", "Generate Unit Tests"};
        analysisTypeComboBox = createStyledComboBox(analysisTypes);
        controlsPanel.add(analysisTypeComboBox);

        controlsPanel.add(createStyledLabel("AI Model:"));
        String[] models = {"nvidia/nemotron-nano-9b-v2:free", "openai/gpt-4o-mini", "openchat/openchat-7b:free", "google/gemini-2.0-flash-001", "x-ai/grok-code-fast-1"};
        modelComboBox = createStyledComboBox(models);
        controlsPanel.add(modelComboBox);

        analyzeButton = createStyledButton("Analyze");
        copyButton = createStyledButton("Copy");
        historyButton = createStyledButton("View History"); // New history button
        controlsPanel.add(analyzeButton);
        controlsPanel.add(copyButton);
        controlsPanel.add(historyButton);

        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(200, 25));
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        progressBar.setFont(UI_FONT.deriveFont(Font.BOLD));
        progressBar.setForeground(ACCENT_COLOR_BLUE);
        progressBar.setBackground(COMPONENT_BG_COLOR);
        progressBar.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        controlsPanel.add(progressBar);

        // Add components to frame
        mainPanel.add(splitPane, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
        add(controlsPanel, BorderLayout.SOUTH);

        // Action Listeners
        analyzeButton.addActionListener(e -> analyzeCodeAction());
        copyButton.addActionListener(e -> copyOutputToClipboard());
        historyButton.addActionListener(e -> showHistoryDialog()); // Listener for history button
    }

    private JPanel createTextPanel(String title, Component textComponent) {
        JPanel panel = new JPanel(new BorderLayout());
        TitledBorder border = new TitledBorder(title);
        border.setTitleFont(UI_FONT.deriveFont(Font.BOLD));
        border.setTitleColor(FOREGROUND_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                new EmptyBorder(5, 5, 5, 5)
        ));
        panel.setBackground(BG_COLOR);

        if (textComponent instanceof JTextArea || textComponent instanceof JEditorPane) {
            textComponent.setFont(CODE_FONT);
            textComponent.setBackground(TEXT_AREA_BG_COLOR);
            textComponent.setForeground(FOREGROUND_COLOR);
        }

        if (textComponent instanceof JTextArea) {
            JTextArea textArea = (JTextArea) textComponent;
            textArea.setCaretColor(Color.BLACK); // Adjusted for light theme
            textArea.setMargin(new Insets(10, 10, 10, 10));
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
        }

        if (textComponent instanceof JEditorPane) {
            ((JEditorPane) textComponent).setCaretColor(Color.BLACK);
            ((JEditorPane) textComponent).setMargin(new Insets(10, 10, 10, 10));
        }

        JScrollPane scrollPane = new JScrollPane(textComponent);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        scrollPane.getHorizontalScrollBar().setUI(new CustomScrollBarUI());
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(FOREGROUND_COLOR);
        label.setFont(UI_FONT);
        return label;
    }

    private JComboBox<String> createStyledComboBox(String[] items) {
        JComboBox<String> comboBox = new JComboBox<>(items);
        comboBox.setFont(UI_FONT);
        // FlatLaf handles styling, so avoid custom colors
        return comboBox;
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(UI_FONT.deriveFont(Font.BOLD));
        button.setFocusPainted(false);
        // Let FlatLaf handle button styling; simplify hover effect
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            public void mouseExited(MouseEvent evt) {
                button.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
        return button;
    }

    private String getSampleCode() {
        return "// Paste any code here! Try Python, Java, JavaScript, etc.\n" +
                "function factorial(n) {\n" +
                "  if (n < 0) {\n" +
                "    return \"Number must be non-negative.\";\n" +
                "  }\n" +
                "  if (n === 0 || n === 1) {\n" +
                "    return 1;\n" +
                "  }\n" +
                "  return n * factorial(n - 1);\n" +
                "}";
    }

    private void analyzeCodeAction() {
        String code = inputCodeArea.getText();
        if (code.trim().isEmpty()) {
            showErrorDialog("Please enter some code to analyze.", "Input Required");
            return;
        }
        if (API_KEY == null || API_KEY.isBlank()) {
            showErrorDialog("Please set the OPENROUTER_API_KEY environment variable before running.", "API Key Missing");
            return;
        }

        setControlsEnabled(false);
        progressBar.setIndeterminate(true);
        progressBar.setString("Contacting AI...");
        outputResultArea.setText(buildHtmlWrapper("Analyzing... Please wait."));

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                String analysisType = (String) analysisTypeComboBox.getSelectedItem();
                String model = (String) modelComboBox.getSelectedItem();
                return callOpenRouterAPI(code, analysisType, model);
            }

            @Override
            protected void done() {
                try {
                    String response = get();
                    outputResultArea.setText(buildHtmlWrapper(response));

                    // Add to history (DSA: Append to ArrayList)
                    String analysisType = (String) analysisTypeComboBox.getSelectedItem();
                    String model = (String) modelComboBox.getSelectedItem();
                    history.add(new HistoryEntry(code, analysisType, model, response));
                } catch (InterruptedException | ExecutionException ex) {
                    String errorMsg = "API Error: " + ex.getCause().getMessage();
                    outputResultArea.setText(buildHtmlWrapper(errorMsg));
                    showErrorDialog(errorMsg, "API Error");
                } finally {
                    outputResultArea.setCaretPosition(0);
                    setControlsEnabled(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setString("Ready");
                }
            }
        }.execute();
    }

    private String callOpenRouterAPI(String code, String analysisType, String model) throws Exception {
        String prompt = buildPrompt(code, analysisType);
        HttpClient client = HttpClient.newHttpClient();
        String requestBody = String.format("{\"model\": \"%s\", \"messages\": [{\"role\": \"user\", \"content\": \"%s\"}]}", model, escapeJson(prompt));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return parseContentFromResponse(response.body());
        } else {
            throw new RuntimeException("API request failed. Status: " + response.statusCode() + " Body: " + response.body());
        }
    }

    private String parseContentFromResponse(String responseBody) {
        String contentKey = "\"content\":\"";
        int start = responseBody.indexOf(contentKey);

        if (start == -1) {
            return "<b>Error:</b> Could not find 'content' key in AI response.<br><pre>" + escapeHtml(responseBody) + "</pre>";
        }

        int startIndex = start + contentKey.length();
        int endIndex = -1;
        int searchIndex = startIndex;

        while ((endIndex = responseBody.indexOf('"', searchIndex)) != -1) {
            if (responseBody.charAt(endIndex - 1) == '\\') {
                searchIndex = endIndex + 1;
            } else {
                break;
            }
        }

        if (endIndex == -1) {
            return "<b>Error:</b> Could not find a closing quote for the content.<br><pre>" + escapeHtml(responseBody) + "</pre>";
        }

        String content = responseBody.substring(startIndex, endIndex);
        return unescapeJson(content);
    }

    private String buildPrompt(String code, String analysisType) {
        String baseInstruction = "You are an expert programmer and code reviewer. Format your entire response using simple HTML. Use <pre><code> for code blocks, <ul> and <li> for lists, and <b> for bold text. Do not include any text outside of the main HTML body content.";
        String task;
        switch (analysisType) {
            case "Find Bugs": task = "Analyze the code for bugs. Provide a list of issues."; break;
            case "Refactor Code": task = "Refactor the code for clarity and efficiency. Provide the refactored code and explain changes."; break;
            case "Add Comments": task = "Add comments to the code. Provide the complete commented code."; break;
            case "Generate Unit Tests": task = "Write unit tests for the code. Provide the complete test code."; break;
            default: task = "Explain the code in simple terms."; break;
        }
        return baseInstruction + "\n\n" + task + "\n\nCode to analyze:\n" + code;
    }

    private void copyOutputToClipboard() {
        String textToCopy = outputResultArea.getText();
        StringSelection stringSelection = new StringSelection(textToCopy);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
        progressBar.setString("Copied to clipboard!");
    }

    private void setControlsEnabled(boolean enabled) {
        analyzeButton.setEnabled(enabled);
        copyButton.setEnabled(enabled);
        historyButton.setEnabled(enabled); // Include history button
        analysisTypeComboBox.setEnabled(enabled);
        modelComboBox.setEnabled(enabled);
        inputCodeArea.setEnabled(enabled);
    }

    private void showErrorDialog(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String unescapeJson(String s) {
        return s.replace("\\\\", "\\").replace("\\\"", "\"").replace("\\n", "\n");
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String buildHtmlWrapper(String content) {
        String style = "body { font-family: Segoe UI, sans-serif; background-color: " + toHex(TEXT_AREA_BG_COLOR) + "; color: " + toHex(FOREGROUND_COLOR) + "; margin: 10px; }" +
                "pre { background-color: " + toHex(BG_COLOR) + "; border: 1px solid " + toHex(BORDER_COLOR) + "; padding: 10px; border-radius: 5px; white-space: pre-wrap; word-wrap: break-word; }" +
                "code { font-family: Consolas, monospace; }" +
                "ul { margin-left: 20px; }";
        return "<html><head><style>" + style + "</style></head><body>" + content + "</body></html>";
    }

    private String toHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    // New method to show history dialog (UI like Grok: Simple list of history entries)
    private void showHistoryDialog() {
        if (history.isEmpty()) {
            showErrorDialog("No history available yet.", "History");
            return;
        }

        JDialog historyDialog = new JDialog(this, "Chat History", true);
        historyDialog.setSize(800, 600);
        historyDialog.setLocationRelativeTo(this);

        // Use JList for simple history display
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (int i = 0; i < history.size(); i++) {
            HistoryEntry entry = history.get(i);
            String summary = String.format("[%d] %s - %s (%s): %s...",
                    i + 1, entry.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    entry.analysisType, entry.model, entry.code.substring(0, Math.min(50, entry.code.length())));
            listModel.addElement(summary);
        }

        JList<String> historyList = new JList<>(listModel);
        historyList.setFont(UI_FONT);
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Double-click to view full entry
        historyList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = historyList.getSelectedIndex();
                    if (index >= 0) {
                        HistoryEntry entry = history.get(index);
                        JDialog detailDialog = new JDialog(historyDialog, "History Entry " + (index + 1), true);
                        detailDialog.setSize(600, 400);
                        detailDialog.setLocationRelativeTo(historyDialog);

                        JTabbedPane tabs = new JTabbedPane();
                        JEditorPane codePane = new JEditorPane();
                        codePane.setText(entry.code);
                        codePane.setEditable(false);
                        codePane.setFont(CODE_FONT);

                        JEditorPane responsePane = new JEditorPane();
                        responsePane.setContentType("text/html");
                        responsePane.setText(buildHtmlWrapper(entry.response));
                        responsePane.setEditable(false);

                        tabs.addTab("Input Code", new JScrollPane(codePane));
                        tabs.addTab("AI Response", new JScrollPane(responsePane));

                        JLabel infoLabel = new JLabel("Type: " + entry.analysisType + " | Model: " + entry.model + " | Time: " + entry.timestamp);
                        infoLabel.setFont(UI_FONT);

                        detailDialog.add(infoLabel, BorderLayout.NORTH);
                        detailDialog.add(tabs, BorderLayout.CENTER);
                        detailDialog.setVisible(true);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(historyList);
        historyDialog.add(scrollPane, BorderLayout.CENTER);
        historyDialog.setVisible(true);
    }

    public static void main(String[] args) {
        // Set FlatLaf Light Look and Feel
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        // Minimal UIManager customizations to align with FlatLaf
        UIManager.put("Panel.background", BG_COLOR);
        UIManager.put("SplitPane.background", BG_COLOR);
        UIManager.put("SplitPaneDivider.background", BG_COLOR);
        SwingUtilities.invokeLater(() -> new PairProgrammer().setVisible(true));
    }

    static class CustomScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = ACCENT_COLOR_BLUE;
            this.trackColor = BG_COLOR;
        }
        @Override
        protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
        @Override
        protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
        private JButton createZeroButton() {
            JButton jbutton = new JButton();
            jbutton.setPreferredSize(new Dimension(0, 0));
            jbutton.setMinimumSize(new Dimension(0, 0));
            jbutton.setMaximumSize(new Dimension(0, 0));
            return jbutton;
        }
    }

    // DSA: Custom class for history entries
    public static class HistoryEntry {
        final String code;
        final String analysisType;
        final String model;
        final String response;
        final LocalDateTime timestamp;

        HistoryEntry(String code, String analysisType, String model, String response) {
            this.code = code;
            this.analysisType = analysisType;
            this.model = model;
            this.response = response;
            this.timestamp = LocalDateTime.now();
        }
    }
}
