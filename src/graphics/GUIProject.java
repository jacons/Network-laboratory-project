package graphics;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import client.Client;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JPasswordField;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JScrollPane;
import javax.swing.JList;

public class GUIProject extends JFrame {
	
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;

	private Client client;
	private JPasswordField passwordField;
	private DefaultListModel<String> listProject = new DefaultListModel<String>();

	
	public static void start(Client client) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUIProject frame = new GUIProject(client);
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	public GUIProject(Client client) {
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				client.logout();
			}
		});
		setType(Type.POPUP);
		setResizable(false);
		setTitle("Worth");
		this.client = client;
		inizialize(); 
		
		client.updateProjectList(listProject);
	}
	private void inizialize() {
		setBackground(Color.WHITE);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 685,384);
		contentPane = new JPanel();
		contentPane.setBackground(Color.WHITE);
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JLabel titleapplication = new JLabel("WORTH: WORkTogetHer");
		titleapplication.setForeground(Color.RED);
		titleapplication.setFont(new Font("Arial Rounded MT Bold", Font.PLAIN, 44));
		titleapplication.setHorizontalAlignment(SwingConstants.CENTER);
		titleapplication.setBounds(5, 10, 661, 52);
		contentPane.add(titleapplication);
		
		JLabel subtitle = new JLabel("Strumento per la gestione di progetti collaborativi che si ispira ad alcuni principi della metodologia Kanban.");
		subtitle.setBackground(Color.WHITE);
		subtitle.setHorizontalAlignment(SwingConstants.CENTER);
		subtitle.setFont(new Font("Tahoma", Font.ITALIC, 13));
		subtitle.setBounds(5, 67, 661, 25);
		contentPane.add(subtitle);
		
		JTextField ProjectField = new JTextField("");
		ProjectField.setFont(new Font("Century Gothic", Font.PLAIN, 15));
		ProjectField.setBounds(239, 146, 254, 26);
		ProjectField.setColumns(10);
		contentPane.add(ProjectField);
		
		JLabel ProjectNameLabel = new JLabel("PROGETTO");
		ProjectNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
		ProjectNameLabel.setFont(new Font("Tahoma", Font.ITALIC, 18));
		ProjectNameLabel.setBounds(20, 147, 209, 22);
		contentPane.add(ProjectNameLabel);
		
		passwordField = new JPasswordField("");
		passwordField.setBounds(458, 260, 196, 25);
		contentPane.add(passwordField);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 194, 344, 150);
		contentPane.add(scrollPane);
		
		
		JList<String> prjlist = new JList<String>(listProject);
		scrollPane.setViewportView(prjlist);		
		
		
		JButton btnNuovoProgetto = new JButton("CREA NUOVO");
		btnNuovoProgetto.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				short Code = client.addProject(ProjectField.getText(),new String(passwordField.getPassword()));
				if(Code == 28) {
					JOptionPane.showMessageDialog(null,"Progetto creato!");
					client.updateProjectList(listProject);

				} else JOptionPane.showMessageDialog(null,client.codeToText(Code,""));
			}
		});
		btnNuovoProgetto.setForeground(Color.BLACK);
		btnNuovoProgetto.setFont(new Font("Tahoma", Font.PLAIN, 19));
		btnNuovoProgetto.setBounds(458, 216, 196, 33);
		contentPane.add(btnNuovoProgetto);
		
		JButton btnModifaProgettoEsistente = new JButton("APRI PROGETTO");
		btnModifaProgettoEsistente.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
								
				short Code = client.openProject(ProjectField.getText());
				if(Code == 28) {
					GUIApplication.start(client);
					
					btnModifaProgettoEsistente.setVisible(false);
					btnNuovoProgetto.setVisible(false);
					
				}else JOptionPane.showMessageDialog(null,client.codeToText(Code,""));
			}
		});
		btnModifaProgettoEsistente.setForeground(Color.BLACK);
		btnModifaProgettoEsistente.setFont(new Font("Tahoma", Font.PLAIN, 14));
		btnModifaProgettoEsistente.setBounds(503, 143, 151, 33);
		contentPane.add(btnModifaProgettoEsistente);
		
		JLabel lblBentornato = new JLabel("Bentornato "+client.getUsername()+", cosa vuoi fare?");
		lblBentornato.setHorizontalAlignment(SwingConstants.CENTER);
		lblBentornato.setFont(new Font("Tahoma", Font.ITALIC, 18));
		lblBentornato.setBounds(5, 102, 661, 22);
		contentPane.add(lblBentornato);
	
		
		JLabel lblIserisciPasswordPer = new JLabel("Inserisci password per cancellare");
		lblIserisciPasswordPer.setHorizontalAlignment(SwingConstants.CENTER);
		lblIserisciPasswordPer.setFont(new Font("Tahoma", Font.PLAIN, 12));
		lblIserisciPasswordPer.setBounds(458, 283, 196, 18);
		contentPane.add(lblIserisciPasswordPer);				
	}
}