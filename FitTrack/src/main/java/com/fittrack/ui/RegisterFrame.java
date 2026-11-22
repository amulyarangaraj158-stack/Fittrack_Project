package com.fittrack.ui;

import com.fittrack.model.User;
import com.fittrack.service.UserService;
import com.fittrack.util.UITheme;
import com.fittrack.util.ValidationUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Registration screen allowing new Regular or Premium users to create an account.
 */
public class RegisterFrame extends JFrame {

    private final UserService userService;

    private JTextField nameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JComboBox<String> accountTypeCombo;
    private JTextField ageField;
    private JComboBox<String> genderCombo;
    private JTextField heightField;
    private JTextField weightField;

    public RegisterFrame() {
        this.userService = new UserService();
        initComponents();
    }

    private void initComponents() {
        setTitle("FitTrack — Create Account");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(760, 680);
        setMinimumSize(new Dimension(700, 620));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.MAIN_BG);

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(UITheme.MAIN_BG);
        wrapper.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        UITheme.RoundedPanel card = new UITheme.RoundedPanel(18);
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));
        card.setPreferredSize(new Dimension(600, 580));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.weightx = 0.5;

        JLabel title = UITheme.createPageTitle("Create Your FitTrack Account");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        card.add(title, gbc);

        JLabel subtitle = new JLabel("Set up your personal fitness profile to start tracking");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_SECONDARY);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 8, 14, 8);
        card.add(subtitle, gbc);

        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 8, 3, 8);

        // Row 2: Full Name & Email
        gbc.gridy = 2;
        gbc.gridx = 0;
        card.add(UITheme.createFieldLabel("Full Name *"), gbc);
        gbc.gridx = 1;
        card.add(UITheme.createFieldLabel("Email Address *"), gbc);

        nameField = UITheme.createTextField();
        emailField = UITheme.createTextField();
        gbc.gridy = 3;
        gbc.gridx = 0;
        card.add(nameField, gbc);
        gbc.gridx = 1;
        card.add(emailField, gbc);

        // Row 4: Password & Confirm Password
        gbc.gridy = 4;
        gbc.gridx = 0;
        card.add(UITheme.createFieldLabel("Password (min 6 chars) *"), gbc);
        gbc.gridx = 1;
        card.add(UITheme.createFieldLabel("Confirm Password *"), gbc);

        passwordField = UITheme.createPasswordField();
        confirmPasswordField = UITheme.createPasswordField();
        gbc.gridy = 5;
        gbc.gridx = 0;
        card.add(passwordField, gbc);
        gbc.gridx = 1;
        card.add(confirmPasswordField, gbc);

        // Row 6: Account Type & Gender
        gbc.gridy = 6;
        gbc.gridx = 0;
        card.add(UITheme.createFieldLabel("Membership Tier"), gbc);
        gbc.gridx = 1;
        card.add(UITheme.createFieldLabel("Gender"), gbc);

        accountTypeCombo = new JComboBox<>(new String[]{User.ACCOUNT_REGULAR, User.ACCOUNT_PREMIUM});
        UITheme.styleComboBox(accountTypeCombo);
        genderCombo = new JComboBox<>(new String[]{"Male", "Female", "Other", "Not Specified"});
        UITheme.styleComboBox(genderCombo);

        gbc.gridy = 7;
        gbc.gridx = 0;
        card.add(accountTypeCombo, gbc);
        gbc.gridx = 1;
        card.add(genderCombo, gbc);

        // Row 8: Age, Height, Weight
        gbc.gridy = 8;
        gbc.gridx = 0;
        card.add(UITheme.createFieldLabel("Age (years) *"), gbc);
        gbc.gridx = 1;
        card.add(UITheme.createFieldLabel("Height (cm) *"), gbc);

        ageField = UITheme.createTextField();
        ageField.setText("20");
        heightField = UITheme.createTextField();
        heightField.setText("170");
        gbc.gridy = 9;
        gbc.gridx = 0;
        card.add(ageField, gbc);
        gbc.gridx = 1;
        card.add(heightField, gbc);

        gbc.gridy = 10;
        gbc.gridx = 0;
        card.add(UITheme.createFieldLabel("Weight (kg) *"), gbc);

        weightField = UITheme.createTextField();
        weightField.setText("70");
        gbc.gridy = 11;
        gbc.gridx = 0;
        card.add(weightField, gbc);

        // Buttons row
        JPanel buttonPanel = new JPanel(new BorderLayout(12, 0));
        buttonPanel.setOpaque(false);

        JButton backBtn = UITheme.createSecondaryButton("Back to Login");
        backBtn.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });

        JButton registerBtn = UITheme.createPrimaryButton("Create Account");
        registerBtn.setPreferredSize(new Dimension(200, 40));
        registerBtn.addActionListener(e -> handleRegister());

        buttonPanel.add(backBtn, BorderLayout.WEST);
        buttonPanel.add(registerBtn, BorderLayout.EAST);

        gbc.gridy = 12;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(18, 8, 4, 8);
        card.add(buttonPanel, gbc);

        wrapper.add(card);
        JScrollPane scrollPane = new JScrollPane(wrapper);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        getRootPane().setDefaultButton(registerBtn);
    }

    private void handleRegister() {
        try {
            String fullName = nameField.getText().trim();
            String email = emailField.getText().trim();
            String password = new String(passwordField.getPassword());
            String confirm = new String(confirmPasswordField.getPassword());
            String accountType = (String) accountTypeCombo.getSelectedItem();
            String gender = (String) genderCombo.getSelectedItem();

            if (!password.equals(confirm)) {
                throw new IllegalArgumentException("Passwords do not match.");
            }

            int age = ValidationUtils.parsePositiveInt(ageField.getText(), "Age");
            double heightCm = ValidationUtils.parsePositiveDouble(heightField.getText(), "Height");
            double weightKg = ValidationUtils.parsePositiveDouble(weightField.getText(), "Weight");

            User createdUser = userService.register(
                    fullName, email, password, accountType, age, gender, heightCm, weightKg
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Registration successful! Welcome to FitTrack, " + createdUser.getFullName() + ".",
                    "Account Created",
                    JOptionPane.INFORMATION_MESSAGE
            );
            dispose();
            new DashboardFrame(createdUser).setVisible(true);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Registration failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
