package graphics;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.border.TitledBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.JSlider;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import client.Client;

import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class GUIApplication extends JFrame {


	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField MsgSendField;
	private JTextField NewCardField;
	private JTextField MoveCardFileld;
	private JTextField removeCardField;
	private JTable CardTable;
	private JTextField usernameNewbemberFiled;
	private JPasswordField passwordField;
	
	private DefaultTableModel CardListModel ;
	private DefaultTableModel HistoryCardModel ;


	private Client client;
	private JTextField historyCardField;
	private JTable table;
	
	private JTextArea msgTab;
	
	public static void start(Client client) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUIApplication frame = new GUIApplication(client);
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	public GUIApplication(Client client) {
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
		
		msgTab = new JTextArea();
		this.client.readytoChat(msgTab);
		
		CardListModel = new DefaultTableModel(
			new Object[][] { }, new String[] { "Nome", "Prorit\u00E0", "Stato", "Descrizione" } ) {
			private static final long serialVersionUID = 1L;
			boolean[] columnEditables = new boolean[] {false, false, false, false};
			public boolean isCellEditable(int row, int column) {return columnEditables[column];}
		};

		HistoryCardModel = new DefaultTableModel(
			new Object[][] { }, new String[] { "Lista partenza", "Lista destinazione", "Data" }) {
			private static final long serialVersionUID = 1L;
			boolean[] columnEditables = new boolean[] { false, false, false };
			public boolean isCellEditable(int row, int column) {return columnEditables[column];}
		};
			
		inizialize(); 
	}
	private void inizialize() {
		setBackground(Color.WHITE);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 790,559);
		contentPane = new JPanel();
		contentPane.setBackground(Color.WHITE);
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JLabel titleapplication = new JLabel("WORTH: WORkTogetHer");
		titleapplication.setBounds(5, 10, 771, 52);
		titleapplication.setForeground(Color.RED);
		titleapplication.setFont(new Font("Arial Rounded MT Bold", Font.PLAIN, 44));
		titleapplication.setHorizontalAlignment(SwingConstants.CENTER);
		contentPane.add(titleapplication);
		
		JLabel subtitle = new JLabel("Strumento per la gestione di progetti collaborativi che si ispira ad alcuni principi della metodologia Kanban.");
		subtitle.setBounds(5, 67, 771, 25);
		subtitle.setBackground(Color.WHITE);
		subtitle.setHorizontalAlignment(SwingConstants.CENTER);
		subtitle.setFont(new Font("Tahoma", Font.ITALIC, 13));
		contentPane.add(subtitle);
		
		JLabel lblLavoraInsiemeAl = new JLabel("Bentornato "+client.getUsername()+", lavora insieme al progetto, \""+client.getNameProject()+"\"");
		lblLavoraInsiemeAl.setBounds(5, 102, 771, 35);
		lblLavoraInsiemeAl.setForeground(Color.BLUE);
		lblLavoraInsiemeAl.setHorizontalAlignment(SwingConstants.CENTER);
		lblLavoraInsiemeAl.setFont(new Font("Sitka Text", Font.PLAIN, 17));
		lblLavoraInsiemeAl.setBackground(Color.WHITE);
		contentPane.add(lblLavoraInsiemeAl);
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setBounds(5, 147, 771, 374);
		tabbedPane.setBackground(Color.WHITE);
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		tabbedPane.setFont(new Font("Arial", Font.PLAIN, 16));
		tabbedPane.setBorder(null);
		contentPane.add(tabbedPane);
		
		JPanel CardsTab = new JPanel();
		CardsTab.setBackground(new Color(255, 235, 205));
		CardsTab.setBorder(null);
		tabbedPane.addTab("Modifica Cards", null, CardsTab, null);
		CardsTab.setLayout(null);
		
		JPanel AddCardPanel = new JPanel();
		AddCardPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(0, 0, 0)), "Aggiungi Cards", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		AddCardPanel.setBounds(10, 10, 366, 321);
		AddCardPanel.setBackground(new Color(255, 235, 205));
		CardsTab.add(AddCardPanel);
		AddCardPanel.setLayout(null);
		
		NewCardField = new JTextField();
		NewCardField.setBounds(10, 50, 227, 22);
		AddCardPanel.add(NewCardField);
		NewCardField.setColumns(10);
		
		JTextArea DescriptonFilelds = new JTextArea();
		DescriptonFilelds.setLineWrap(true);
		DescriptonFilelds.setRows(2);
		DescriptonFilelds.setToolTipText("");
		DescriptonFilelds.setBounds(10, 102, 346, 83);
		AddCardPanel.add(DescriptonFilelds);
		
		JSlider PrioritySlider = new JSlider();
		PrioritySlider.setValue(1);
		PrioritySlider.setMaximum(5);
		PrioritySlider.setMinimum(1);
		PrioritySlider.setBackground(new Color(255, 235, 205));
		PrioritySlider.setBounds(109, 195, 247, 22);
		AddCardPanel.add(PrioritySlider);
		
		JLabel lblPriorit = new JLabel("Priorit\u00E0");
		lblPriorit.setHorizontalAlignment(SwingConstants.CENTER);
		lblPriorit.setBounds(10, 197, 94, 20);
		AddCardPanel.add(lblPriorit);
		
		JLabel lblDescrizione = new JLabel("Descrizione");
		lblDescrizione.setHorizontalAlignment(SwingConstants.LEFT);
		lblDescrizione.setBounds(10, 82, 346, 20);
		AddCardPanel.add(lblDescrizione);
		
		JLabel lblNomeCard = new JLabel("Nome Card");
		lblNomeCard.setHorizontalAlignment(SwingConstants.LEFT);
		lblNomeCard.setBounds(10, 31, 227, 20);
		AddCardPanel.add(lblNomeCard);
		
		JButton AddCardBtn = new JButton("Aggiungi");
		AddCardBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				short Code = client.addCard(NewCardField.getText(),DescriptonFilelds.getText(),PrioritySlider.getValue());
				JOptionPane.showMessageDialog(null,client.codeToText(Code,"Card aggiunta."));
			}
		});
		AddCardBtn.setForeground(new Color(255, 255, 255));
		AddCardBtn.setBackground(new Color(67, 80, 88));
		AddCardBtn.setFont(new Font("Tahoma", Font.PLAIN, 12));
		AddCardBtn.setBounds(10, 251, 346, 41);
		AddCardPanel.add(AddCardBtn);
		
		JPanel MoveCardPanel = new JPanel();
		MoveCardPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(0, 0, 0)), "Sposta Cards", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		MoveCardPanel.setBackground(new Color(255, 235, 205));
		MoveCardPanel.setBounds(386, 10, 366, 176);
		CardsTab.add(MoveCardPanel);
		MoveCardPanel.setLayout(null);
		
		JLabel lblNomeCard_1 = new JLabel("Nome Card");
		lblNomeCard_1.setHorizontalAlignment(SwingConstants.LEFT);
		lblNomeCard_1.setBounds(10, 23, 227, 20);
		MoveCardPanel.add(lblNomeCard_1);
		
		MoveCardFileld = new JTextField();
		MoveCardFileld.setColumns(10);
		MoveCardFileld.setBounds(10, 42, 227, 22);
		MoveCardPanel.add(MoveCardFileld);
		
		JRadioButton from_todo = new JRadioButton("To do");
		from_todo.setBackground(new Color(255, 235, 205));
		from_todo.setBounds(10, 91, 138, 21);
		MoveCardPanel.add(from_todo);
		
		JRadioButton from_inprgrs = new JRadioButton("In Progress");
		from_inprgrs.setBackground(new Color(255, 235, 205));
		from_inprgrs.setBounds(10, 114, 138, 21);
		MoveCardPanel.add(from_inprgrs);
		
		JRadioButton from_tobersvrd = new JRadioButton("To Be Revised");
		from_tobersvrd.setBackground(new Color(255, 235, 205));
		from_tobersvrd.setBounds(10, 137, 138, 21);
		MoveCardPanel.add(from_tobersvrd);
		
		
		ButtonGroup from_bg=new ButtonGroup();from_bg.add(from_inprgrs);
		from_bg.add(from_tobersvrd);from_bg.add(from_todo);

		JRadioButton to_prgrs = new JRadioButton("In Progress");
		to_prgrs.setBackground(new Color(255, 235, 205));
		to_prgrs.setBounds(218, 91, 138, 21);
		MoveCardPanel.add(to_prgrs);
		
		JRadioButton to_tobersvrd = new JRadioButton("To Be Revised");
		to_tobersvrd.setBackground(new Color(255, 235, 205));
		to_tobersvrd.setBounds(218, 114, 138, 21);
		MoveCardPanel.add(to_tobersvrd);
		
		JRadioButton to_Done = new JRadioButton("Done");
		to_Done.setBackground(new Color(255, 235, 205));
		to_Done.setBounds(218, 137, 138, 21);
		MoveCardPanel.add(to_Done);
		
		ButtonGroup to_bg=new ButtonGroup();to_bg.add(to_prgrs);
		to_bg.add(to_tobersvrd);to_bg.add(to_Done);
		
		JButton MoveCardBtn = new JButton("Sposta");
		MoveCardBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				int from = -1,to = -1;
				if(from_todo.isSelected()) from = 0;			
				else if(from_inprgrs.isSelected()) from = 1; 
				else if(from_tobersvrd.isSelected()) from = 2;

				if(to_prgrs.isSelected()) to = 1;			
				else if(to_tobersvrd.isSelected()) to = 2; 
				else if(to_Done.isSelected()) to = 3;			
				
				short Code = client.moveCard(MoveCardFileld.getText(), from, to);
				JOptionPane.showMessageDialog(null,client.codeToText(Code,"Card spostata."));
				
			}
		});
		MoveCardBtn.setForeground(Color.WHITE);
		MoveCardBtn.setFont(new Font("Tahoma", Font.PLAIN, 12));
		MoveCardBtn.setBackground(new Color(67, 80, 88));
		MoveCardBtn.setBounds(247, 23, 109, 56);
		MoveCardPanel.add(MoveCardBtn);
		
	
		
		JLabel lblNomeCard_1_2 = new JLabel("Da");
		lblNomeCard_1_2.setHorizontalAlignment(SwingConstants.CENTER);
		lblNomeCard_1_2.setBounds(10, 68, 89, 20);
		MoveCardPanel.add(lblNomeCard_1_2);
		
		JLabel lblNomeCard_1_2_1 = new JLabel("A");
		lblNomeCard_1_2_1.setHorizontalAlignment(SwingConstants.CENTER);
		lblNomeCard_1_2_1.setBounds(148, 68, 89, 20);
		MoveCardPanel.add(lblNomeCard_1_2_1);
		
		JPanel deleteCards = new JPanel();
		deleteCards.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(0, 0, 0)), "Rimuovi Cards", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		deleteCards.setBackground(new Color(255, 235, 205));
		deleteCards.setBounds(386, 196, 366, 135);
		CardsTab.add(deleteCards);
		deleteCards.setLayout(null);
		
		removeCardField = new JTextField();
		removeCardField.setColumns(10);
		removeCardField.setBounds(10, 61, 227, 22);
		deleteCards.add(removeCardField);
		
		JLabel lblNomeCard_1_1 = new JLabel("Nome Card");
		lblNomeCard_1_1.setHorizontalAlignment(SwingConstants.LEFT);
		lblNomeCard_1_1.setBounds(10, 43, 227, 20);
		deleteCards.add(lblNomeCard_1_1);
		
		JButton RemoveCardBtn = new JButton("Elimina");
		RemoveCardBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				short Code = client.deleteCard(removeCardField.getText());
				JOptionPane.showMessageDialog(null,client.codeToText(Code,"Card eliminata."));			
			}
		});
		RemoveCardBtn.setForeground(Color.WHITE);
		RemoveCardBtn.setFont(new Font("Tahoma", Font.PLAIN, 12));
		RemoveCardBtn.setBackground(new Color(67, 80, 88));
		RemoveCardBtn.setBounds(247, 43, 109, 56);
		deleteCards.add(RemoveCardBtn);
		
		JPanel ShowCard = new JPanel();
		ShowCard.setBackground(new Color(255, 235, 205));
		ShowCard.setBorder(null);
		tabbedPane.addTab("Mostra Cards", null, ShowCard, null);
		ShowCard.setLayout(null);
		
		JPanel showCards = new JPanel();
		showCards.setLayout(null);
		showCards.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(0, 0, 0)), "Mostra Cards", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		showCards.setBackground(new Color(255, 235, 205));
		showCards.setBounds(10, 10, 746, 321);
		ShowCard.add(showCards);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 60, 726, 251);
		showCards.add(scrollPane);
		
		CardTable = new JTable();
		CardTable.setModel(CardListModel);
		CardTable.getColumnModel().getColumn(0).setResizable(false);
		CardTable.getColumnModel().getColumn(0).setPreferredWidth(90);
		CardTable.getColumnModel().getColumn(0).setMinWidth(90);
		CardTable.getColumnModel().getColumn(0).setMaxWidth(90);
		CardTable.getColumnModel().getColumn(1).setResizable(false);
		CardTable.getColumnModel().getColumn(1).setPreferredWidth(50);
		CardTable.getColumnModel().getColumn(1).setMinWidth(50);
		CardTable.getColumnModel().getColumn(1).setMaxWidth(50);
		CardTable.getColumnModel().getColumn(2).setResizable(false);
		CardTable.getColumnModel().getColumn(2).setMinWidth(75);
		CardTable.getColumnModel().getColumn(2).setMaxWidth(75);
		CardTable.getColumnModel().getColumn(3).setResizable(false);
		CardTable.getColumnModel().getColumn(3).setPreferredWidth(500);
		CardTable.getColumnModel().getColumn(3).setMinWidth(600);
		CardTable.getColumnModel().getColumn(3).setMaxWidth(600);
		CardTable.setShowVerticalLines(false);
		CardTable.setShowHorizontalLines(false);
		CardTable.setShowGrid(false);
		CardTable.setBackground(Color.WHITE);
		scrollPane.setViewportView(CardTable);
		
		JButton btnMostra = new JButton("Mostra");
		btnMostra.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				short Code = client.showCards(CardListModel);
				if(Code!=28)JOptionPane.showMessageDialog(null,client.codeToText(Code,""));
			}
		});
		btnMostra.setForeground(Color.WHITE);
		btnMostra.setFont(new Font("Tahoma", Font.PLAIN, 12));
		btnMostra.setBackground(new Color(67, 80, 88));
		btnMostra.setBounds(10, 20, 726, 29);
		showCards.add(btnMostra);
		
		JPanel ShowCard_1 = new JPanel();
		ShowCard_1.setLayout(null);
		ShowCard_1.setBorder(null);
		ShowCard_1.setBackground(new Color(255, 235, 205));
		tabbedPane.addTab("Cronogia Card", null, ShowCard_1, null);
		
		JPanel historycard_1 = new JPanel();
		historycard_1.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(0, 0, 0)), "Mostra Cronoglia Card", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		historycard_1.setBackground(new Color(255, 235, 205));
		historycard_1.setBounds(10, 11, 746, 321);
		ShowCard_1.add(historycard_1);
		historycard_1.setLayout(null);
		
		historyCardField = new JTextField();
		historyCardField.setBounds(10, 38, 607, 22);
		historyCardField.setColumns(10);
		historycard_1.add(historyCardField);
		
		JButton btnMostra_1_1 = new JButton("Mostra");
		btnMostra_1_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				short Code = client.historyCard(historyCardField.getText(),HistoryCardModel);
				if(Code!=28)JOptionPane.showMessageDialog(null,client.codeToText(Code,""));
			}
		});
		btnMostra_1_1.setBounds(627, 19, 109, 41);
		btnMostra_1_1.setForeground(Color.WHITE);
		btnMostra_1_1.setFont(new Font("Tahoma", Font.PLAIN, 12));
		btnMostra_1_1.setBackground(new Color(67, 80, 88));
		historycard_1.add(btnMostra_1_1);
		
		JLabel lblNomeCard_2_1 = new JLabel("Nome Card");
		lblNomeCard_2_1.setBounds(10, 19, 188, 20);
		lblNomeCard_2_1.setHorizontalAlignment(SwingConstants.LEFT);
		historycard_1.add(lblNomeCard_2_1);
		
		JScrollPane scrollPane_1 = new JScrollPane();
		scrollPane_1.setBounds(10, 71, 714, 239);
		historycard_1.add(scrollPane_1);
		
		table = new JTable();
		table.setModel(HistoryCardModel);
		table.getColumnModel().getColumn(0).setResizable(false);
		table.getColumnModel().getColumn(0).setPreferredWidth(240);
		table.getColumnModel().getColumn(0).setMinWidth(240);
		table.getColumnModel().getColumn(0).setMaxWidth(240);
		table.getColumnModel().getColumn(1).setResizable(false);
		table.getColumnModel().getColumn(1).setPreferredWidth(240);
		table.getColumnModel().getColumn(1).setMinWidth(240);
		table.getColumnModel().getColumn(1).setMaxWidth(240);
		table.getColumnModel().getColumn(2).setResizable(false);
		table.getColumnModel().getColumn(2).setPreferredWidth(240);
		table.getColumnModel().getColumn(2).setMinWidth(240);
		table.getColumnModel().getColumn(2).setMaxWidth(240);
		scrollPane_1.setViewportView(table);

		
		JPanel Membres = new JPanel();
		Membres.setBackground(new Color(255, 235, 205));
		Membres.setBorder(null);
		tabbedPane.addTab("Il mio Team", null, Membres, null);
		Membres.setLayout(null);
		
		JPanel listofmember = new JPanel();
		listofmember.setLayout(null);
		listofmember.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(0, 0, 0)), "Team del progetto", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		listofmember.setBackground(new Color(255, 235, 205));
		listofmember.setBounds(10, 10, 366, 321);
		Membres.add(listofmember);
		
		DefaultListModel<String> listMembers = new DefaultListModel<String>();

		JList<String> memberlist = new JList<String>(listMembers);
		memberlist.setBounds(10, 50, 346, 261);
		listofmember.add(memberlist);
		
		JButton btnUpdateMemList = new JButton("Carica/Aggiorna");
		btnUpdateMemList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				short Code = client.updateMember(listMembers);
				if(Code!=28)JOptionPane.showMessageDialog(null,client.codeToText(Code,""));
			}
		});
		btnUpdateMemList.setForeground(Color.WHITE);
		btnUpdateMemList.setFont(new Font("Tahoma", Font.PLAIN, 12));
		btnUpdateMemList.setBackground(new Color(67, 80, 88));
		btnUpdateMemList.setBounds(10, 20, 346, 23);
		listofmember.add(btnUpdateMemList);
		
		JPanel newmember = new JPanel();
		newmember.setLayout(null);
		newmember.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(0, 0, 0)), "Aggiungi Membri al team", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		newmember.setBackground(new Color(255, 235, 205));
		newmember.setBounds(386, 10, 366, 135);
		Membres.add(newmember);
		
		usernameNewbemberFiled = new JTextField();
		usernameNewbemberFiled.setColumns(10);
		usernameNewbemberFiled.setBounds(10, 55, 227, 22);
		newmember.add(usernameNewbemberFiled);
		
		JLabel usernameLabel = new JLabel("Username");
		usernameLabel.setHorizontalAlignment(SwingConstants.LEFT);
		usernameLabel.setBounds(10, 33, 227, 20);
		newmember.add(usernameLabel);
		
		JButton btnAggiugniMembro = new JButton("Aggiugni Membro");
		btnAggiugniMembro.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				short Code = client.addMember(usernameNewbemberFiled.getText());
				if(Code!=28)JOptionPane.showMessageDialog(null,client.codeToText(Code,""));
				else btnUpdateMemList.doClick();
			}
		});
		btnAggiugniMembro.setForeground(Color.WHITE);
		btnAggiugniMembro.setFont(new Font("Tahoma", Font.PLAIN, 12));
		btnAggiugniMembro.setBackground(new Color(67, 80, 88));
		btnAggiugniMembro.setBounds(184, 87, 172, 38);
		newmember.add(btnAggiugniMembro);
		
		passwordField = new JPasswordField();
		passwordField.setBounds(386, 283, 207, 19);
		Membres.add(passwordField);
		
		JButton btnEliminaProgetto = new JButton("Elimina progetto");
		btnEliminaProgetto.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				short Code = client.deleteProject(new String(passwordField.getPassword())); 
				JOptionPane.showMessageDialog(null,client.codeToText(Code,"Progetto eliminato"));
			}
		});
		btnEliminaProgetto.setForeground(Color.WHITE);
		btnEliminaProgetto.setFont(new Font("Tahoma", Font.PLAIN, 12));
		btnEliminaProgetto.setBackground(new Color(67, 80, 88));
		btnEliminaProgetto.setBounds(599, 264, 157, 38);
		Membres.add(btnEliminaProgetto);
		
		JLabel lblInserireLaPassw = new JLabel("Inserire la passw per eliminare");
		lblInserireLaPassw.setHorizontalAlignment(SwingConstants.LEFT);
		lblInserireLaPassw.setBounds(386, 264, 207, 20);
		Membres.add(lblInserireLaPassw);
		
		JLabel info_label = new JLabel("Una volta cancellato il progetto, verrai disconnesso.");
		info_label.setHorizontalAlignment(SwingConstants.CENTER);
		info_label.setBounds(386, 305, 366, 20);
		Membres.add(info_label);
		
		JPanel ChatTab = new JPanel();
		ChatTab.setBackground(new Color(255, 235, 205));
		ChatTab.setBorder(null);
		tabbedPane.addTab("Chat/Updates", null, ChatTab, null);
		ChatTab.setLayout(null);
		
		JScrollPane scrollPane_2 = new JScrollPane();
		scrollPane_2.setBounds(10, 11, 746, 275);
		ChatTab.add(scrollPane_2);
		
		msgTab.setEditable(false);
		scrollPane_2.setViewportView(msgTab);
		
		MsgSendField = new JTextField();
		MsgSendField.setBounds(10, 298, 610, 25);
		ChatTab.add(MsgSendField);
		MsgSendField.setColumns(10);
		
		JButton btnSendMessage = new JButton("Invia");
		btnSendMessage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				short Code = client.sendMessage(MsgSendField.getText());
				if(Code!=28) JOptionPane.showMessageDialog(null,client.codeToText(Code,""));
				else MsgSendField.setText("");
			}
		});
		btnSendMessage.setFont(new Font("Arial", Font.PLAIN, 17));
		btnSendMessage.setBounds(630, 298, 126, 25);
		ChatTab.add(btnSendMessage);		
	}
}
