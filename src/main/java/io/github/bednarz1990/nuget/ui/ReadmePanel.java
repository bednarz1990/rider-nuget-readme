package io.github.bednarz1990.nuget.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.Disposable;
import com.intellij.ui.components.*;
import com.intellij.util.concurrency.AppExecutorUtil;
import io.github.bednarz1990.nuget.core.*;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class ReadmePanel extends JPanel implements Disposable {
    private final NuGetClient client = new NuGetClient(new NuGetHttp());
    private final ExecutorService executor = AppExecutorUtil.createBoundedApplicationPoolExecutor("NuGet README", 2);
    private final LatestRequest requests = new LatestRequest();
    private final JBTextField query = new JBTextField();
    private final JCheckBox prerelease = new JCheckBox("Prerelease");
    private final DefaultListModel<PackageInfo> packages = new DefaultListModel<>();
    private final JBList<PackageInfo> list = new JBList<>(packages);
    private final JComboBox<String> versions = new JComboBox<>();
    private final JEditorPane preview = new JEditorPane();
    private final JTextArea source = new JTextArea();
    private final JLabel status = new JLabel("Search NuGet.org to read documentation before installing.");
    private final JLabel title = new JLabel("No package selected");
    private final JButton retry = new JButton("Retry");
    private final JButton copy = new JButton("Copy Markdown");
    private final JButton open = new JButton("Open on NuGet.org");
    private final JProgressBar progress = new JProgressBar();
    private final PropertyChangeListener theme = e -> { if ("lookAndFeel".equals(e.getPropertyName())) SwingUtilities.invokeLater(this::updatePreview); };
    private volatile boolean disposed;
    private boolean updating;
    private Runnable retryAction;
    private PackageInfo selected;
    private String safeHtml = "<p>Search for a package, then select its version.</p>";

    public ReadmePanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        query.getEmptyText().setText("Package name or keyword (e.g. Serilog)");
        query.getAccessibleContext().setAccessibleName("Search NuGet.org packages");
        JButton search = new JButton("Search");
        JPanel bar = new JPanel(new BorderLayout(6, 0));
        bar.add(query, BorderLayout.CENTER);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        controls.add(prerelease); controls.add(search); bar.add(controls, BorderLayout.EAST);
        add(bar, BorderLayout.NORTH);
        search.addActionListener(e -> search()); query.addActionListener(e -> search()); prerelease.addActionListener(e -> search());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.getAccessibleContext().setAccessibleName("Package search results");
        list.addListSelectionListener(e -> { if (!e.getValueIsAdjusting() && !updating) select(list.getSelectedValue()); });
        versions.getAccessibleContext().setAccessibleName("Package version");
        versions.addActionListener(e -> { if (!updating && versions.getSelectedItem() != null) loadReadme(); });
        preview.setEditable(false); preview.setEditorKit(new HTMLEditorKit());
        preview.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        preview.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && SafeMarkdown.safeLink(e.getDescription())) BrowserUtil.browse(e.getDescription());
        });
        source.setEditable(false); source.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Preview", new JBScrollPane(preview)); tabs.addTab("Markdown", new JBScrollPane(source));
        JPanel details = new JPanel(new BorderLayout(5, 5));
        JPanel heading = new JPanel(new BorderLayout(5, 5)); heading.add(title, BorderLayout.CENTER); heading.add(versions, BorderLayout.EAST);
        details.add(heading, BorderLayout.NORTH); details.add(tabs, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        actions.add(copy); actions.add(open);
        JButton clear = new JButton("Clear cache"); actions.add(clear);
        clear.addActionListener(e -> { client.clearCache(); if (selected != null && versions.getSelectedItem() != null) loadReadme(); });
        copy.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(source.getText()), null));
        open.addActionListener(e -> {
            if (selected != null && versions.getSelectedItem() != null) BrowserUtil.browse("https://www.nuget.org/packages/" + selected.id() + "/" + versions.getSelectedItem());
        });
        details.add(actions, BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JBScrollPane(list), details);
        split.setResizeWeight(.25); split.setDividerLocation(180); add(split, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(6, 0));
        footer.add(status, BorderLayout.CENTER); footer.add(retry, BorderLayout.EAST);
        progress.setIndeterminate(true); progress.setPreferredSize(new Dimension(50, 5)); footer.add(progress, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);
        retry.addActionListener(e -> { if (retryAction != null) retryAction.run(); });
        retry.setVisible(false); progress.setVisible(false); copy.setEnabled(false); open.setEnabled(false);
        UIManager.addPropertyChangeListener(theme);
        updatePreview();
    }

    private void search() {
        String term = query.getText().trim();
        if (term.isEmpty()) { status.setText("Enter a package name or keyword."); return; }
        boolean includePre = prerelease.isSelected();
        updating = true;
        packages.clear(); versions.removeAllItems(); selected = null;
        updating = false;
        title.setText("Search results"); resetContent();
        run("Searching NuGet.org…", this::search, () -> client.search(term, includePre), results -> {
            updating = true; results.forEach(packages::addElement); updating = false;
            status.setText(results.isEmpty() ? "No packages found. Try a different search." : results.size() + " results (maximum 50). Select a package.");
        });
    }

    private void select(PackageInfo info) {
        requests.next(); selected = info; resetContent();
        updating = true; versions.removeAllItems(); updating = false;
        if (info == null) return;
        title.setText(info.id()); title.setToolTipText(null); // Do not interpret untrusted descriptions as Swing HTML.
        boolean includePre = prerelease.isSelected();
        run("Loading versions…", () -> select(info), () -> client.versions(info.id(), includePre), items -> {
            updating = true; items.forEach(versions::addItem);
            if (items.contains(info.version())) versions.setSelectedItem(info.version());
            updating = false;
            if (items.isEmpty()) status.setText("No matching versions. Try enabling prerelease."); else loadReadme();
        });
    }

    private record Rendered(Readme readme, String html) {}
    private void loadReadme() {
        if (selected == null || versions.getSelectedItem() == null) return;
        String id = selected.id(), version = versions.getSelectedItem().toString();
        title.setText(id + " · " + version); resetContent();
        run("Loading README…", this::loadReadme, () -> {
            Readme readme = client.readme(id, version);
            return new Rendered(readme, readme.available() ? SafeMarkdown.render(readme.markdown()) : "<p>This version does not declare a package README.</p>");
        }, rendered -> {
            safeHtml = rendered.html(); source.setText(rendered.readme().markdown()); source.setCaretPosition(0);
            updatePreview(); copy.setEnabled(rendered.readme().available()); open.setEnabled(true);
            status.setText(rendered.readme().available() ? "README ready. Select text to copy. Remote images are disabled." : "No README available for this version.");
        });
    }

    private void resetContent() {
        source.setText(""); safeHtml = "<p>Select a package version to preview its README.</p>";
        copy.setEnabled(false); open.setEnabled(false); updatePreview();
    }

    private <T> void run(String message, Runnable again, Callable<T> work, Consumer<T> success) {
        long ticket = requests.next(); retryAction = again;
        status.setText(message); retry.setVisible(false); progress.setVisible(true);
        Future<?> future = executor.submit(() -> {
            try {
                T result = work.call();
                SwingUtilities.invokeLater(() -> {
                    if (!disposed && requests.current(ticket)) { progress.setVisible(false); success.accept(result); }
                });
            } catch (InterruptedException | CancellationException e) { Thread.currentThread().interrupt(); }
            catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    if (!disposed && requests.current(ticket)) {
                        progress.setVisible(false); retry.setVisible(true);
                        String messageText = e.getMessage() == null ? "Unable to load data. Check your connection." : e.getMessage();
                        // JLabel auto-renders HTML when a string begins with <html>.
                        status.setText("Error: " + messageText.substring(0, Math.min(180, messageText.length())));
                    }
                });
            }
        });
        requests.attach(ticket, future);
    }

    private void updatePreview() {
        if (disposed) return;
        Color fg = UIManager.getColor("Label.foreground"), bg = UIManager.getColor("Panel.background");
        if (fg == null) fg = Color.BLACK;
        if (bg == null) bg = Color.WHITE;
        String foreground = String.format("#%06x", fg.getRGB() & 0xffffff);
        String background = String.format("#%06x", bg.getRGB() & 0xffffff);
        preview.setText("<html><head><style>body { font-family: sans-serif; color:" + foreground + "; background:" + background
                + "; margin: 12px; } pre,code { font-family: monospace; } pre { padding: 8px; } td,th { padding: 5px; } a { color: #589df6; }</style></head><body>" + safeHtml + "</body></html>");
        preview.setBackground(bg); preview.setForeground(fg); preview.setCaretPosition(0);
    }

    @Override public void dispose() {
        disposed = true; requests.next(); executor.shutdownNow(); client.clearCache(); UIManager.removePropertyChangeListener(theme);
    }
}
