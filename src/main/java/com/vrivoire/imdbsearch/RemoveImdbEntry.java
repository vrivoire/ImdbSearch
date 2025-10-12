package com.vrivoire.imdbsearch;

import com.vrivoire.imdbsearch.log4j.LogGrabberAppender;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.regex.Pattern;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RemoveImdbEntry extends JFrame implements ActionListener {

    private static final long serialVersionUID = -2733088726337040679L;
    private static final Logger LOG = LogManager.getLogger(Main.class);
    private JFrame frame;
    private JTextField textField;
    private JLabel label;

    static {
        System.setProperty("-J-Djava.util.Arrays.useLegacyMergeSort", "true");
        LogGrabberAppender.setPanel(new JTextArea());
        Config.configure();
    }

    public RemoveImdbEntry() {

    }

    public static void main(String[] args) {
        RemoveImdbEntry te = new RemoveImdbEntry();
        te.showDialog();
//		te.dbAction();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String s = e.getActionCommand();
        LOG.info("-> " + s);
//		if (s.equals("submit")) {
        label.setText(textField.getText());
//		textField.setText("  ");
        DbUtils.dbDelete(textField.getText(), frame);
//		}
    }

    void showDialog() {
//		frameLogs.setLocationRelativeTo(null);
        frame = new JFrame("Remove Imdb Entry");
        JPanel panel = new JPanel();

        textField = new JTextField(16);
        ((AbstractDocument) textField.getDocument()).setDocumentFilter(new RemoveImdbEntry.CustomDocumentFilter());
        textField.setFont(new Font("Lucida Console", Font.PLAIN, 12));
        textField.addActionListener(this);
        panel.add(textField);

        JButton button = new JButton("Submit");
        button.addActionListener(this);
        panel.add(button);

        label = new JLabel(" ");
        panel.add(label);

        frame.add(panel);
        URL imageURL = Main.class.getResource(Config.ICON.getString());
        if (imageURL != null) {
            frame.setIconImage(new ImageIcon(imageURL).getImage());
        }
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(150, 125);
        frame.getContentPane().add(panel, BorderLayout.CENTER);
//		frame.setAlwaysOnTop(true);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private class CustomDocumentFilter extends DocumentFilter {

        private static final Pattern regexCheck = Pattern.compile("[0-9]+");

        @Override
        public void insertString(DocumentFilter.FilterBypass fb, int offs, String str, AttributeSet a) throws BadLocationException {
            if (str == null) {
                return;
            }
            if (regexCheck.matcher(str).matches()) {
                super.insertString(fb, offs, str, a);
            }
        }

        @Override
        public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String str, AttributeSet attrs) throws BadLocationException {
            if (str == null) {
                return;
            }
            if (regexCheck.matcher(str).matches()) {
                fb.replace(offset, length, str, attrs);
            }
        }
    }
}
