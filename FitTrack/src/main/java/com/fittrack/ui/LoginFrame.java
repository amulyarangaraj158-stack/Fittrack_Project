package com.fittrack.ui;

import com.fittrack.model.User;
import com.fittrack.service.UserService;
import com.fittrack.util.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Login screen for FitTrack desktop application.
 */
public class LoginFrame extends JFrame {

    private final UserService userService;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JCheckBox showPasswordCheck;

    public LoginFrame() {
        this.userService = new UserService();
        initComponents();
    }

    private void initComponents() {
        setTitle("FitTrack — Sign In");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(920, 580);
        setMinimumSize(new Dimension(860, 540));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Left Navy Brand Panel
        JPanel brandPanel = new JPanel();
        brandPanel.setBackground(UITheme.SIDEBAR_BG);
        brandPanel.setPreferredSize(new Dimension(360, 580));
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setBorder(BorderFactory.createEmptyBorder(55, 40, 40, 40));

        JLabel badge = new JLabel("FITTRACK DESKTOP");
        badge.setFont(new Font("SansSerif", Font.BOLD, 12));
        badge.setForeground(UITheme.ACCENT_GREEN);
        badge.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("<html>Personal Fitness<br>&amp; Activity Tracker</html>");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("<html>Track workouts, daily steps, hydration, and fitness goals with real-time SQLite analytics.</html>");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_MUTED_LIGHT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        brandPanel.add(badge);
        brandPanel.add(Box.createVerticalStrut(14));
        brandPanel.add(title);
        brandPanel.add(Box.createVerticalStrut(16));
        brandPanel.add(subtitle);
        brandPanel.add(Box.createVerticalStrut(32));

        String[] highlights = {
                "•  5 Polymorphic Workout Categories",
                "•  Automated Calorie Estimation",
                "•  Daily Water & Step Goal Progress",
                "•  Interactive JFreeChart Reports"
        };
        for (String item : highlights) {
            JLabel lbl = new JLabel(item);
            lbl.setFont(UITheme.FONT_BODY);
            lbl.setForeground(new Color(203, 213, 225));
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            brandPanel.add(lbl);
            brandPanel.add(Box.createVerticalStrut(10));
        }

        brandPanel.add(Box.createVerticalGlue());

        JLabel demoHint = new JLabel("<html><b>Demo Account:</b><br>alex@fittrack.com / FitTrack@123</html>");
        demoHint.setFont(UITheme.FONT_SMALL);
        demoHint.setForeground(UITheme.TEXT_MUTED_LIGHT);
        demoHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandPanel.add(demoHint);

        // Right Login Form Container
        JPanel rightContainer = new JPanel(new GridBagLayout());
        rightContainer.setBackground(UITheme.MAIN_BG);

        UITheme.RoundedPanel card = new UITheme.RoundedPanel(18);
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(32, 36, 32, 36));
        card.setPreferredSize(new Dimension(420, 440));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        JLabel loginHeading = new JLabel("Welcome Back");
        loginHeading.setFont(UITheme.FONT_TITLE);
        loginHeading.setForeground(UITheme.TEXT_PRIMARY);
        gbc.gridy = 0;
        card.add(loginHeading, gbc);

        JLabel loginSub = new JLabel("Sign in to your FitTrack account");
        loginSub.setFont(UITheme.FONT_BODY);
        loginSub.setForeground(UITheme.TEXT_SECONDARY);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 18, 0);
        card.add(loginSub, gbc);

        gbc.insets = new Insets(4, 0, 4, 0);
        gbc.gridy = 2;
        card.add(UITheme.createFieldLabel("Email Address"), gbc);

        emailField = UITheme.createTextField();
        gbc.gridy = 3;
        card.add(emailField, gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(12, 0, 4, 0);
        card.add(UITheme.createFieldLabel("Password"), gbc);

        passwordField = UITheme.createPasswordField();
        gbc.gridy = 5;
        gbc.insets = new Insets(4, 0, 4, 0);
        card.add(passwordField, gbc);

        showPasswordCheck = new JCheckBox("Show password");
        showPasswordCheck.setFont(UITheme.FONT_SMALL);
        showPasswordCheck.setForeground(UITheme.TEXT_SECONDARY);
        showPasswordCheck.setOpaque(false);
        showPasswordCheck.addActionListener(e -> {
            passwordField.setEchoChar(showPasswordCheck.isSelected() ? (char) 0 : '\u2022');
        });
        gbc.gridy = 6;
        card.add(showPasswordCheck, gbc);

        JButton loginBtn = UITheme.createPrimaryButton("Sign In");
        loginBtn.setPreferredSize(new Dimension(220, 40));
        loginBtn.addActionListener(e -> handleLogin());
        gbc.gridy = 7;
        gbc.insets = new Insets(16, 0, 8, 0);
        card.add(loginBtn, gbc);

        JPanel actionRow = new JPanel(new BorderLayout(8, 0));
        actionRow.setOpaque(false);

        JButton demoBtn = UITheme.createSecondaryButton("Fill Demo User");
        demoBtn.addActionListener(e -> {
            emailField.setText("alex@fittrack.com");
            passwordField.setText("FitTrack@123");
        });

        JButton registerBtn = UITheme.createBlueButton("Create Account");
        registerBtn.addActionListener(e -> openRegisterScreen());

        actionRow.add(demoBtn, BorderLayout.WEST);
        actionRow.add(registerBtn, BorderLayout.EAST);

        gbc.gridy = 8;
        gbc.insets = new Insets(6, 0, 0, 0);
        card.add(actionRow, gbc);

        rightContainer.add(card);

        add(brandPanel, BorderLayout.WEST);
        add(rightContainer, BorderLayout.CENTER);

        getRootPane().setDefaultButton(loginBtn);
    }

    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        try {
            User user = userService.login(email, password);
            dispose();
            new DashboardFrame(user).setVisible(true);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Login Validation", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database error during login: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openRegisterScreen() {
        dispose();
        new RegisterFrame().setVisible(true);
    }
}
