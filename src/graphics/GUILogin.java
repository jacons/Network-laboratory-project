package graphics;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

import java.awt.Font;
import javax.swing.JTextField;
import javax.swing.JButton;
import java.awt.event.ActionListener;

import java.awt.event.ActionEvent;
import java.awt.Color;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import client.Client;

public class GUILogin {

	private JFrame LoginForm;
	private JTextField userField;
	private JPasswordField passwordField;

	private Client client;	
	
	public static void start(Client client) {
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUILogin window = new GUILogin(client);
					window.LoginForm.setVisible(true);
				} catch (Exception e) { 
					JOptionPane.showMessageDialog(null,e.getMessage());
					System.exit(1);
				}	
			}
		});
	}
	
	public GUILogin(Client client) {
		this.client = client;
		initialize(); 
	}
	
	private void initialize() {
		
		LoginForm = new JFrame();
		LoginForm.getContentPane().setBackground(Color.WHITE);
		LoginForm.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) { client.logout(); }
		});
		LoginForm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		LoginForm.setBackground(Color.WHITE);
		LoginForm.setTitle("Worth");
		LoginForm.setResizable(false);
		LoginForm.setBounds(100, 100, 685, 485);
		LoginForm.getContentPane().setLayout(null);
	
		JLabel titleapplication = new JLabel("WORTH: WORkTogetHer");
		titleapplication.setForeground(Color.RED);
		titleapplication.setFont(new Font("Arial Rounded MT Bold", Font.PLAIN, 44));
		titleapplication.setHorizontalAlignment(SwingConstants.CENTER);
		titleapplication.setBounds(10, 10, 659, 87);
		LoginForm.getContentPane().add(titleapplication);
		
		JLabel subtitle = new JLabel("Strumento per la gestione di progetti collaborativi che si ispira ad alcuni principi della metodologia Kanban.");
		subtitle.setHorizontalAlignment(SwingConstants.CENTER);
		subtitle.setFont(new Font("Tahoma", Font.ITALIC, 13));
		subtitle.setBounds(20, 85, 649, 25);
		LoginForm.getContentPane().add(subtitle);
		
		JLabel form1 = new JLabel("Accedi ed incomincia ad organizzarti con i tuoi amici");
		form1.setFont(new Font("Tahoma", Font.PLAIN, 12));
		form1.setBounds(10, 149, 286, 18);
		LoginForm.getContentPane().add(form1);
		
		userField = new JTextField("");
		userField.setFont(new Font("Century Gothic", Font.PLAIN, 15));
		userField.setBounds(24, 209, 257, 25);
		LoginForm.getContentPane().add(userField);
		userField.setColumns(10);
		
		passwordField = new JPasswordField("");
		passwordField.setBounds(24, 276, 257, 25);
		LoginForm.getContentPane().add(passwordField);
		 
		JLabel lblNewLabel = new JLabel("USERNAME");
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel.setFont(new Font("Tahoma", Font.ITALIC, 18));
		lblNewLabel.setBounds(24, 177, 257, 22);
		LoginForm.getContentPane().add(lblNewLabel);
		
		JLabel lblPassword = new JLabel("PASSWORD");
		lblPassword.setHorizontalAlignment(SwingConstants.CENTER);
		lblPassword.setFont(new Font("Tahoma", Font.ITALIC, 18));
		lblPassword.setBounds(24, 244, 257, 22);
		LoginForm.getContentPane().add(lblPassword);
		
	
		JButton btnlogout = new JButton("EXIT");
		btnlogout.setForeground(Color.RED);
		btnlogout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) { client.logout();}
		});
		btnlogout.setFont(new Font("Tahoma", Font.PLAIN, 15));
		btnlogout.setBounds(194, 409, 87, 33);
		btnlogout.setVisible(false);
		LoginForm.getContentPane().add(btnlogout);
		
		JButton btnsignup = new JButton("SIGN UP");
		btnsignup.setForeground(Color.BLACK);
		btnsignup.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				short Code = client.register(userField.getText(),new String(passwordField.getPassword()));
				JOptionPane.showMessageDialog(null,client.codeToText(Code,"Utente creato."));
			}
		});
		btnsignup.setFont(new Font("Tahoma", Font.PLAIN, 19));
		btnsignup.setBounds(24, 323, 130, 33);
		LoginForm.getContentPane().add(btnsignup);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(306, 149, 363, 293);
		LoginForm.getContentPane().add(scrollPane);

		DefaultTableModel tablemodel = new DefaultTableModel(new Object[][] {},new String[] {"Utenti", "Stato"});
		JTable table = new JTable();
		table.setBackground(Color.WHITE);
		table.setShowVerticalLines(false);
		table.setShowHorizontalLines(false);
		table.setShowGrid(false);
		table.setModel(tablemodel);
		scrollPane.setViewportView(table);
		
		JButton btnlogin = new JButton("LOGIN");
		btnlogin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				short Code = client.login(userField.getText(),new String(passwordField.getPassword()),tablemodel);
				
				if(Code==28) {
					
					btnsignup.setVisible(false);
					btnlogin.setVisible(false);
					btnlogout.setVisible(true);
					
					GUIProject.start(client);
					
				} else JOptionPane.showMessageDialog(null,client.codeToText(Code,""));
	
		}});
		btnlogin.setFont(new Font("Tahoma", Font.PLAIN, 19));
		btnlogin.setBounds(166, 323, 130, 33);
		LoginForm.getContentPane().add(btnlogin);			 
	}	
}