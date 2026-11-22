package com.fittrack.ui;

import com.fittrack.model.User;
import com.fittrack.service.UserService;
import com.fittrack.util.UITheme;
import com.fittrack.util.ValidationUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.function.Consumer;

/**
 * User Profile screen for viewing/updating personal metrics and viewing informational BMI.
 */
public class ProfilePanel extends JPanel {

    private User currentUser;
    private final UserService userService;
    private final Consumer<User> onProfileUpdated;

    private JTextField nameField;
    private JTextField emailField;
    private JComboBox<String> tierCombo;
    private JTextField ageField;
    private JComboBox<String> genderCombo;
    private JTextField heightField;
    private JTextField weightField;

    private JLabel badgeLabel;
    private JLabel benefitsLabel;
    private JLabel memberSinceLabel;
    private JLabel bmiValueLabel;
    private JLabel bmiCategoryLabel;

    public ProfilePanel(User user, Consumer<User> onProfileUpdated) {
        this.currentUser = user;
        this.userService = new UserService();
        this.onProfileUpdated = onProfileUpdated;
        initComponents();
        loadProfileData();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadProfileData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 20));
        setBackground(UITheme.MAIN_BG);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        header.add(UITheme.createPageTitle("User Profile & Body Metrics"), BorderLayout.NORTH);
        JLabel sub = new JLabel("Manage your personal information, membership tier, and body metrics");
        sub.setFont(UITheme.FONT_BODY);
        sub.setForeground(UITheme.TEXT_SECONDARY);
        header.add(sub, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(1, 2, 20, 0));
        grid.setOpaque(false);

        // Left: Profile Form Card
        UITheme.RoundedPanel formCard = new UITheme.RoundedPanel(16);
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.weightx = 0.5;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        formCard.add(UITheme.createSectionTitle("Personal Details"), gbc);

        gbc.gridwidth = 1;
        nameField = UITheme.createTextField();
        emailField = UITheme.createTextField();
        emailField.setEditable(false);
        emailField.setBackground(new Color(241, 245, 249));

        tierCombo = new JComboBox<>(new String[]{User.ACCOUNT_REGULAR, User.ACCOUNT_PREMIUM});
        UITheme.styleComboBox(tierCombo);

        ageField = UITheme.createTextField();
        genderCombo = new JComboBox<>(new String[]{"Male", "Female", "Other", "Not Specified"});
        UITheme.styleComboBox(genderCombo);

        heightField = UITheme.createTextField();
        weightField = UITheme.createTextField();

        gbc.gridy = 1;
        gbc.gridx = 0;
        formCard.add(UITheme.createFieldLabel("Full Name *"), gbc);
        gbc.gridx = 1;
        formCard.add(UITheme.createFieldLabel("Email (Read-Only)"), gbc);

        gbc.gridy = 2;
        gbc.gridx = 0;
        formCard.add(nameField, gbc);
        gbc.gridx = 1;
        formCard.add(emailField, gbc);

        gbc.gridy = 3;
        gbc.gridx = 0;
        formCard.add(UITheme.createFieldLabel("Membership Tier"), gbc);
        gbc.gridx = 1;
        formCard.add(UITheme.createFieldLabel("Gender"), gbc);

        gbc.gridy = 4;
        gbc.gridx = 0;
        formCard.add(tierCombo, gbc);
        gbc.gridx = 1;
        formCard.add(genderCombo, gbc);

        gbc.gridy = 5;
        gbc.gridx = 0;
        formCard.add(UITheme.createFieldLabel("Age *"), gbc);
        gbc.gridx = 1;
        formCard.add(UITheme.createFieldLabel("Height (cm) *"), gbc);

        gbc.gridy = 6;
        gbc.gridx = 0;
        formCard.add(ageField, gbc);
        gbc.gridx = 1;
        formCard.add(heightField, gbc);

        gbc.gridy = 7;
        gbc.gridx = 0;
        formCard.add(UITheme.createFieldLabel("Weight (kg) *"), gbc);

        gbc.gridy = 8;
        gbc.gridx = 0;
        formCard.add(weightField, gbc);

        JButton saveBtn = UITheme.createPrimaryButton("Save Profile Changes");
        saveBtn.addActionListener(e -> handleSaveProfile());
        gbc.gridy = 9;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(16, 6, 4, 6);
        formCard.add(saveBtn, gbc);

        // Right: Account Summary & Informational BMI Card
        JPanel rightStack = new JPanel(new GridLayout(2, 1, 0, 18));
        rightStack.setOpaque(false);

        UITheme.RoundedPanel accountCard = new UITheme.RoundedPanel(16);
        accountCard.setLayout(new BoxLayout(accountCard, BoxLayout.Y_AXIS));
        accountCard.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));

        JLabel accTitle = UITheme.createSectionTitle("Account Membership");
        accTitle.setAlignmentX(LEFT_ALIGNMENT);
        badgeLabel = new JLabel();
        badgeLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        badgeLabel.setForeground(UITheme.ACCENT_GREEN_DARK);
        badgeLabel.setAlignmentX(LEFT_ALIGNMENT);

        benefitsLabel = new JLabel();
        benefitsLabel.setFont(UITheme.FONT_BODY);
        benefitsLabel.setForeground(UITheme.TEXT_SECONDARY);
        benefitsLabel.setAlignmentX(LEFT_ALIGNMENT);

        memberSinceLabel = new JLabel();
        memberSinceLabel.setFont(UITheme.FONT_SMALL);
        memberSinceLabel.setForeground(UITheme.TEXT_SECONDARY);
        memberSinceLabel.setAlignmentX(LEFT_ALIGNMENT);

        accountCard.add(accTitle);
        accountCard.add(Box.createVerticalStrut(12));
        accountCard.add(badgeLabel);
        accountCard.add(Box.createVerticalStrut(8));
        accountCard.add(benefitsLabel);
        accountCard.add(Box.createVerticalStrut(14));
        accountCard.add(memberSinceLabel);

        UITheme.RoundedPanel bmiCard = new UITheme.RoundedPanel(16);
        bmiCard.setLayout(new BoxLayout(bmiCard, BoxLayout.Y_AXIS));
        bmiCard.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));

        JLabel bmiTitle = UITheme.createSectionTitle("BMI Informational Metric");
        bmiTitle.setAlignmentX(LEFT_ALIGNMENT);

        bmiValueLabel = new JLabel("0.0");
        bmiValueLabel.setFont(new Font("SansSerif", Font.BOLD, 34));
        bmiValueLabel.setForeground(UITheme.ACCENT_BLUE);
        bmiValueLabel.setAlignmentX(LEFT_ALIGNMENT);

        bmiCategoryLabel = new JLabel();
        bmiCategoryLabel.setFont(UITheme.FONT_BODY_BOLD);
        bmiCategoryLabel.setForeground(UITheme.TEXT_PRIMARY);
        bmiCategoryLabel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel bmiNote = new JLabel("<html>Formula: weight (kg) / [height (m)]<sup>2</sup>. Displayed strictly as a general informational metric; not a clinical assessment.</html>");
        bmiNote.setFont(UITheme.FONT_SMALL);
        bmiNote.setForeground(UITheme.TEXT_SECONDARY);
        bmiNote.setAlignmentX(LEFT_ALIGNMENT);

        bmiCard.add(bmiTitle);
        bmiCard.add(Box.createVerticalStrut(10));
        bmiCard.add(bmiValueLabel);
        bmiCard.add(Box.createVerticalStrut(6));
        bmiCard.add(bmiCategoryLabel);
        bmiCard.add(Box.createVerticalStrut(12));
        bmiCard.add(bmiNote);

        rightStack.add(accountCard);
        rightStack.add(bmiCard);

        grid.add(formCard);
        grid.add(rightStack);
        add(grid, BorderLayout.CENTER);
    }

    public void loadProfileData() {
        nameField.setText(currentUser.getFullName());
        emailField.setText(currentUser.getEmail());
        tierCombo.setSelectedItem(currentUser.getAccountType());
        ageField.setText(String.valueOf(currentUser.getAge()));
        genderCombo.setSelectedItem(currentUser.getGender());
        heightField.setText(String.valueOf(currentUser.getHeightCm()));
        weightField.setText(String.valueOf(currentUser.getWeightKg()));

        badgeLabel.setText(currentUser.getMembershipBadge());
        benefitsLabel.setText("<html>" + currentUser.getMembershipBenefits() + "</html>");
        memberSinceLabel.setText("Member since: " + (currentUser.getCreatedAt() != null ? currentUser.getCreatedAt() : "Today"));

        bmiValueLabel.setText(String.format("%.1f", currentUser.calculateBMI()));
        bmiCategoryLabel.setText(currentUser.getBmiCategory());
    }

    private void handleSaveProfile() {
        try {
            String fullName = nameField.getText().trim();
            String accountType = (String) tierCombo.getSelectedItem();
            String gender = (String) genderCombo.getSelectedItem();
            int age = ValidationUtils.parsePositiveInt(ageField.getText(), "Age");
            double heightCm = ValidationUtils.parsePositiveDouble(heightField.getText(), "Height");
            double weightKg = ValidationUtils.parsePositiveDouble(weightField.getText(), "Weight");

            User updated = userService.updateProfile(
                    currentUser, fullName, accountType, age, gender, heightCm, weightKg
            );
            this.currentUser = updated;
            loadProfileData();
            onProfileUpdated.accept(updated);
            JOptionPane.showMessageDialog(this, "Profile updated successfully.",
                    "Profile Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not update profile: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
