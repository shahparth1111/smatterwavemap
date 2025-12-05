package app.smatter.smatterwavemap;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.JLabel;
import javax.swing.JButton;

public class UTCheck extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					UTCheck frame = new UTCheck();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public UTCheck() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(0, 0, 1920, 1080);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
	    Dimension size= Toolkit.getDefaultToolkit().getScreenSize();
	    int width = (int)size.getWidth();
	    int height = (int)size.getHeight();
		JLabel lblNewLabel = new JLabel("90%");
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER); 
		lblNewLabel.setFont(new FontUIResource(new Font("Cabin", Font.PLAIN, 60)));

		lblNewLabel.setForeground(Color.white);
		lblNewLabel.setBounds(width/2, height-(height/8), 300, 100);
		contentPane.add(lblNewLabel);
		
		JButton btnNewButton = new JButton("Get Audio File Overlap");
		btnNewButton.setBounds(202, 267, 200, 30);
		contentPane.add(btnNewButton);

	}
}
