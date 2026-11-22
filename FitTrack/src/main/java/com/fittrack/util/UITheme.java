package com.fittrack.util;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Central UI design system for FitTrack Swing components.
 * Dark navy sidebar, light slate canvas, crisp white cards, and emerald green accents.
 */
public final class UITheme {

    public static final Color SIDEBAR_BG = new Color(15, 23, 42);        // #0F172A
    public static final Color SIDEBAR_HOVER = new Color(30, 41, 59);     // #1E293B
    public static final Color SIDEBAR_ACTIVE = new Color(30, 41, 59);
    public static final Color MAIN_BG = new Color(241, 245, 249);        // #F1F5F9
    public static final Color CARD_BG = Color.WHITE;
    public static final Color CARD_BORDER = new Color(226, 232, 240);    // #E2E8F0

    public static final Color ACCENT_GREEN = new Color(16, 185, 129);    // #10B981
    public static final Color ACCENT_GREEN_DARK = new Color(5, 150, 105);// #059669
    public static final Color ACCENT_GREEN_SOFT = new Color(209, 250, 229);
    public static final Color ACCENT_BLUE = new Color(59, 130, 246);     // #3B82F6
    public static final Color ACCENT_BLUE_SOFT = new Color(219, 234, 254);
    public static final Color ACCENT_AMBER = new Color(245, 158, 11);    // #F59E0B
    public static final Color ACCENT_AMBER_SOFT = new Color(254, 243, 199);
    public static final Color ACCENT_RED = new Color(239, 68, 68);       // #EF4444
    public static final Color ACCENT_RED_DARK = new Color(220, 38, 38);

    public static final Color TEXT_PRIMARY = new Color(15, 23, 42);      // #0F172A
    public static final Color TEXT_SECONDARY = new Color(100, 116, 139); // #64748B
    public static final Color TEXT_LIGHT = new Color(248, 250, 252);     // #F8FAFC
    public static final Color TEXT_MUTED_LIGHT = new Color(148, 163, 184);

    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 24);
    public static final Font FONT_SUBTITLE = new Font("SansSerif", Font.BOLD, 16);
    public static final Font FONT_KPI = new Font("SansSerif", Font.BOLD, 26);
    public static final Font FONT_BODY = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font("SansSerif", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 12);

    private UITheme() {
    }

    /**
     * Custom JPanel with smooth rounded corners and optional subtle border.
     */
    public static class RoundedPanel extends JPanel {
        private final int cornerRadius;
        private final Color backgroundColor;
        private final Color borderColor;

        public RoundedPanel(int cornerRadius, Color backgroundColor, Color borderColor) {
            this.cornerRadius = cornerRadius;
            this.backgroundColor = backgroundColor;
            this.borderColor = borderColor;
            setOpaque(false);
        }

        public RoundedPanel(int cornerRadius) {
            this(cornerRadius, CARD_BG, CARD_BORDER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth() - 1;
            int h = getHeight() - 1;

            g2.setColor(backgroundColor);
            g2.fillRoundRect(0, 0, w, h, cornerRadius, cornerRadius);

            if (borderColor != null) {
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, w, h, cornerRadius, cornerRadius);
            }
            g2.dispose();
        }
    }

    public static JButton createPrimaryButton(String text) {
        return createStyledButton(text, ACCENT_GREEN, ACCENT_GREEN_DARK, Color.WHITE);
    }

    public static JButton createSecondaryButton(String text) {
        return createStyledButton(text, new Color(226, 232, 240), new Color(203, 213, 225), TEXT_PRIMARY);
    }

    public static JButton createDangerButton(String text) {
        return createStyledButton(text, ACCENT_RED, ACCENT_RED_DARK, Color.WHITE);
    }

    public static JButton createBlueButton(String text) {
        return createStyledButton(text, ACCENT_BLUE, new Color(37, 99, 235), Color.WHITE);
    }

    public static JButton createStyledButton(String text, Color baseColor, Color hoverColor, Color fgColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        button.setFont(FONT_BODY_BOLD);
        button.setForeground(fgColor);
        button.setBackground(baseColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(hoverColor);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(baseColor);
            }
        });
        return button;
    }

    public static JTextField createTextField() {
        JTextField field = new JTextField();
        styleInputBorder(field);
        return field;
    }

    public static JPasswordField createPasswordField() {
        JPasswordField field = new JPasswordField();
        styleInputBorder(field);
        return field;
    }

    public static void styleInputBorder(JTextField field) {
        field.setFont(FONT_BODY);
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(Color.WHITE);
        field.setPreferredSize(new Dimension(220, 36));
        Border line = BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true);
        Border padding = BorderFactory.createEmptyBorder(6, 10, 6, 10);
        field.setBorder(BorderFactory.createCompoundBorder(line, padding));
    }

    public static <T> void styleComboBox(JComboBox<T> combo) {
        combo.setFont(FONT_BODY);
        combo.setBackground(Color.WHITE);
        combo.setForeground(TEXT_PRIMARY);
        combo.setPreferredSize(new Dimension(220, 36));
    }

    public static JProgressBar createProgressBar(Color barColor) {
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setStringPainted(true);
        bar.setFont(FONT_SMALL);
        bar.setForeground(barColor);
        bar.setBackground(new Color(226, 232, 240));
        bar.setBorderPainted(false);
        bar.setPreferredSize(new Dimension(200, 20));
        return bar;
    }

    public static void styleTable(JTable table) {
        table.setFont(FONT_BODY);
        table.setRowHeight(32);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(241, 245, 249));
        table.setSelectionBackground(ACCENT_GREEN_SOFT);
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_BODY_BOLD);
        header.setBackground(SIDEBAR_BG);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(header.getWidth(), 36));
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                }
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return c;
            }
        };
        table.setDefaultRenderer(Object.class, cellRenderer);
    }

    public static JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_BODY_BOLD);
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    public static JLabel createPageTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_TITLE);
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    public static JLabel createSectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_SUBTITLE);
        label.setForeground(TEXT_PRIMARY);
        return label;
    }
}
