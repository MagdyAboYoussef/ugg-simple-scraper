package main;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyGUIApp extends JFrame {
    private List<InputFieldCombo> inputFields;
    private Map<String, Map<String,String>> values;
    private JPanel inputPanel = new JPanel();
    private JButton addButton;
    private JButton submitButton;
    private JTextField excel;
    private JCheckBox update,ranked,headless;

    public Map<String, Map<String,String>> getValues() {
        return values;
    }

    public MyGUIApp() {
        setTitle("Input Fields App");
        setSize(650, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        inputFields = new ArrayList<>();

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));


        addButton = new JButton("+");
        addButton.addActionListener(e -> {
            JTextField newInputField = new JTextField(10);
            JComboBox<String> newComboBox = new JComboBox<>(new DefaultComboBoxModel<>(new String[]{"EUW1","KR", "NA", "EUNE"}));
            JComboBox<String> role = new JComboBox<>(new DefaultComboBoxModel<>(new String[]{"NONE","SUPPORT", "TOP", "MID","JUNGLE","ADC"}));

            InputFieldCombo combo = new InputFieldCombo(newInputField, newComboBox,role);
            inputFields.add(combo);
            inputPanel.add(newInputField);
            inputPanel.add(newComboBox);
            inputPanel.add(role);
            inputPanel.revalidate();
            inputPanel.repaint();
        });

        mainPanel.add(inputPanel, BorderLayout.CENTER);

        submitButton = new JButton("Submit");
        submitButton.addActionListener(e -> {
            try {
                values = new HashMap<>();
                for (InputFieldCombo combo : inputFields) {
                    String name = combo.getInputField().getText();
                    String server = (String) combo.getComboBox().getSelectedItem();
                    String role = (String) combo.getRoleComboBox().getSelectedItem();
                    Map<String, String> innerMap = new HashMap<>();
                    innerMap.put("server", server);
                    innerMap.put("role", role);
                    values.put(getLastWord(name), innerMap);
                }
                String excelValue = excel.getText();
                ugg page = new ugg(values,excelValue,headless.isSelected(),update.isSelected(),ranked.isSelected());

                // Success message
                JOptionPane.showMessageDialog(this, "Success!", "Operation Successful", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception exception) {
                // Convert the stack trace to a String
                StringWriter errors = new StringWriter();
                exception.printStackTrace(new PrintWriter(errors));

                // Create a JTextArea to contain the error
                JTextArea textArea = new JTextArea(errors.toString());
                textArea.setEditable(false);
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);

                // Create a JScrollPane to contain the JTextArea
                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(350, 150));
                scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

                JOptionPane.showMessageDialog(this, scrollPane, "Error", JOptionPane.ERROR_MESSAGE);
            }
        });



        JButton removeButton = new JButton("-");
        removeButton.addActionListener(e -> {
            if (inputFields.size() > 0) {
                InputFieldCombo lastCombo = inputFields.get(inputFields.size() - 1);
                inputFields.remove(lastCombo);
                inputPanel.remove(lastCombo.getInputField());
                inputPanel.remove(lastCombo.getComboBox());
                inputPanel.remove(lastCombo.getRoleComboBox());
                inputPanel.revalidate();
                inputPanel.repaint();
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(submitButton);
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        excel = new JTextField("1",15);

        headless = new JCheckBox("Invisible");
        update = new JCheckBox("Update");
        ranked = new JCheckBox("Ranked");

        buttonPanel.add(headless);
        buttonPanel.add(update);
        buttonPanel.add(ranked);

        buttonPanel.add(excel);


        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
        setVisible(true);
    }



    private class InputFieldCombo {
        private JTextField inputField;
        private JComboBox<String> comboBox;
        private final JComboBox<String> roleComboBox;

        public InputFieldCombo(JTextField inputField, JComboBox<String> comboBox, JComboBox<String> roleComboBox) {
            this.inputField = inputField;
            this.comboBox = comboBox;
            this.roleComboBox = roleComboBox;
        }

        public JTextField getInputField() {
            return inputField;
        }

        JComboBox<String> getComboBox() {
            return comboBox;
        }
        public JComboBox<String> getRoleComboBox() {
            return roleComboBox;
        }
    }

    public static String getLastWord(String inputString) {
        if (inputString.startsWith("https://www.op.gg/")) {
            // Remove the URL prefix
            String[] urlParts = inputString.split("/");
            System.out.println(urlParts);
            inputString = urlParts[urlParts.length-1];
        }

        // Replace '%20' with spaces
        inputString = inputString.replace("%20", " ");

        return inputString;
    }
}
