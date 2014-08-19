package com.saic.uicds.clients.em.capAlertViewer;

import java.io.File;
import java.io.FileNotFoundException;

import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Component;
import java.awt.Event;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;
import java.awt.Point;

import javax.swing.SwingConstants;
import javax.swing.BoxLayout;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JMenuItem;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.Box;
import javax.swing.JTextArea;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.border.Border;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import java.util.List;


public class CapAlertClient implements ActionListener {

    private static final String APP_CONTEXT_FILE = "capAlertSubmitter-context.xml";

	private JFrame jFrame = null;
	private JMenuBar jJMenuBar = null;
	private JMenu fileMenu = null;
	private JMenu editMenu = null;
	private JMenu helpMenu = null;
	private JMenuItem exitMenuItem = null;
	private JMenuItem aboutMenuItem = null;
	private JMenuItem cutMenuItem = null;
	private JMenuItem copyMenuItem = null;
	private JMenuItem pasteMenuItem = null;
	private JMenuItem saveMenuItem = null;
	private JDialog aboutDialog = null;
	private JPanel aboutContentPane = null;
	private JLabel aboutVersionLabel = null;
	
	private JList sampleJList;

	private JButton alertRefreshButton;
	private JButton incidentRefreshButton;
	private JButton createButton;
    private JButton associateButton;

    private JTextArea statusText;
	private	JTable table;

    private JList incidentList;

	private static CapAlertSubmitter caSubmitter;
	String dataValues[][]; 
	String columnNames[] = { "Event", "Headline"};

	TableModel alertTableModel;
    private List<String> incidents;
    private String alertSelected = "";
    private String incidentSelected = "";

	
	/**
     * main entry to the program
	 * @param args
	 */
	public static void main(String[] args) {

        if (args.length == 1) {
            usage();
            return;
        }

        try
        {
            ApplicationContext context = getApplicationContext();

            caSubmitter = (CapAlertSubmitter) context.getBean("capAlertSubmitter");
            if (caSubmitter == null) {
                System.err.println("Could not instantiate CapAlertSubmitter");
            }

            CapAlertClient application = new CapAlertClient();
            application.getJFrame().setVisible(true);

        } catch(Exception e) {
            e.printStackTrace();
        }
	}

    /**
     * Application context
     * find the context config from current location.  If not then look 
     * into the jar file
     */
    private static ApplicationContext getApplicationContext()
        throws Exception {

        System.out.println("in CapAlertClient::getApplicationContext()");

        ApplicationContext context = null;
        try {
            context = new FileSystemXmlApplicationContext("./" + APP_CONTEXT_FILE);
            System.out.println("Using local application context file: " + APP_CONTEXT_FILE);

        } catch (BeansException e) {
            if (e.getCause() instanceof FileNotFoundException) {
                System.out.println("Local application context file not found, using file from jar: contexts/"
                    + APP_CONTEXT_FILE);
            } else {
                System.out.println("Error reading local context file");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        // if file is not found then use the file in the path
        if (context == null) {
            context = new ClassPathXmlApplicationContext(new String[] { "contexts/"
                + APP_CONTEXT_FILE });
        }
        return context;
    }

    private static void usage() {

        System.out.println("");
        System.out.println("This is the UICDS CAP Adapter.");
        System.out.println("Execution of this client depends on a functioning UICDS server. The default is http://localhost/uicds/core/ws/services");
        System.out.println("To verify that a UICDS server is accessible, use a browser to navigate to http://localhost/uicds/Console.html");
        System.out.println("");
        System.out.println("Usage: java -jar capAlertClient.jar");
        System.out.println("");
        System.out.println("Parameters for the tpmAdapter can be configued in Spring context file");
        System.out.println("in the current directory or classpath named: " + APP_CONTEXT_FILE);
    }

	/**
	 * This method create the main frame for the UI
	 * 
	 * @return javax.swing.JFrame
	 */
	private JFrame getJFrame() {

        // create the main frame
		if (jFrame == null) {
			jFrame = new JFrame();
			jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			jFrame.setJMenuBar(getJJMenuBar());
			jFrame.setSize(770, 600);
			
			
			// set the font
	        Font displayFont = new Font("Times New Roman",Font.PLAIN, 14);

            // set the layout
			Container content=jFrame.getContentPane();

            content.setLayout(new BorderLayout());
			

            // create the incident panel
			content.add(createAlertListPanel(displayFont), BorderLayout.WEST);
			content.add(createIncidentListPanel(displayFont), BorderLayout.EAST);
			content.add(createButtonsPanel(displayFont), BorderLayout.SOUTH);

		    jFrame.setVisible(true);
		}
		return jFrame;
	}

    /*
     * create the status panel
     */
    private JPanel createStatusPanel(Font displayFont) 
    {
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
        Border statusPanelBorder =
          BorderFactory.createTitledBorder("Status");
        statusPanel.setBorder(statusPanelBorder);

        statusText = new JTextArea();
        statusText.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusText.setEditable(false);
        statusPanel.setFont(displayFont);
        statusPanel.add(statusText);
        return statusPanel;

    }

    /*
     * Create the alert list panel
     */
    private JPanel createAlertListPanel(Font displayFont)
    {
        JPanel alertsPanel = new JPanel();
        alertsPanel.setLayout(new BoxLayout(alertsPanel, BoxLayout.Y_AXIS));
        Border listPanelBorder =
          BorderFactory.createTitledBorder("CAP Alerts");
        alertsPanel.setBorder(listPanelBorder);

        //load the alert table		
        dataValues = caSubmitter.getListOfAlerts();
        alertTableModel = new DefaultTableModel(dataValues, columnNames);
        table = new JTable (alertTableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel()
            .addListSelectionListener(new AlertRowListener());
        JScrollPane listPane = new JScrollPane(table);
        alertsPanel.add(listPane);

        //create the refresh button
        alertRefreshButton = new JButton("Refresh Alerts");
        alertRefreshButton.addActionListener(this);
        alertsPanel.add(alertRefreshButton);

        return alertsPanel;
    }


    /*
     * Create the incident list panel
     */
    private JPanel createIncidentListPanel(Font displayFont)
    {
        JPanel incidentsPanel = new JPanel();
        incidentsPanel.setLayout(new BoxLayout(incidentsPanel, BoxLayout.Y_AXIS));
        Border listPanelBorder =
            BorderFactory.createTitledBorder("Alert Incidents");
        incidentsPanel.setBorder(listPanelBorder);

        incidents = caSubmitter.getListOfAlertIncidents();

        Object [] incidentsArray = incidents.toArray();
        incidentList = new JList(incidentsArray);
        incidentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        incidentList.getSelectionModel()
            .addListSelectionListener(new IncidentRowListener());
        JScrollPane dsScrollPane = new JScrollPane(incidentList);
        incidentsPanel.add(dsScrollPane);

        //create the refresh button
        incidentRefreshButton = new JButton("Refresh Incidents");
        incidentRefreshButton.addActionListener(this);
        incidentsPanel.add(incidentRefreshButton);

        return incidentsPanel;
    }

    /*
     * Create the buttons panel, which consist of add file, add link, add datasource
     * and unregister datasource
     */
    private JPanel createButtonsPanel(Font displayFont)
    {
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
     
        createButton = new JButton("Create Incident from Alert");
        createButton.addActionListener(this);
        createButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        createButton.setEnabled(false);
        buttonsPanel.add(createButton);

        associateButton = new JButton("Associate Alert with Incident");
        associateButton.addActionListener(this);
        associateButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        associateButton.setEnabled(false);
        buttonsPanel.add(associateButton);


        buttonsPanel.add(createStatusPanel(displayFont));
        return buttonsPanel;
    }


	/**
	 * This method initializes jJMenuBar	
	 * 	
	 * @return javax.swing.JMenuBar	
	 */
	private JMenuBar getJJMenuBar() {
		if (jJMenuBar == null) {
			jJMenuBar = new JMenuBar();
			jJMenuBar.add(getFileMenu());
			jJMenuBar.add(getHelpMenu());
		}
		return jJMenuBar;
	}

	/**
	 * This method initializes jMenu	
	 * 	
	 * @return javax.swing.JMenu	
	 */
	private JMenu getFileMenu() {
		if (fileMenu == null) {
			fileMenu = new JMenu();
			fileMenu.setText("File");
			fileMenu.add(getExitMenuItem());
		}
		return fileMenu;
	}

	/**
	 * This method initializes jMenu	
	 * 	
	 * @return javax.swing.JMenu	
	 */
	private JMenu getHelpMenu() {
		if (helpMenu == null) {
			helpMenu = new JMenu();
			helpMenu.setText("Help");
			helpMenu.add(getAboutMenuItem());
		}
		return helpMenu;
	}

	/**
	 * This method initializes jMenuItem	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getExitMenuItem() {
		if (exitMenuItem == null) {
			exitMenuItem = new JMenuItem();
			exitMenuItem.setText("Exit");
			exitMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					System.exit(0);
				}
			});
		}
		return exitMenuItem;
	}

	/**
	 * This method initializes jMenuItem	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getAboutMenuItem() {
		if (aboutMenuItem == null) {
			aboutMenuItem = new JMenuItem();
			aboutMenuItem.setText("About");
			aboutMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JDialog aboutDialog = getAboutDialog();
					aboutDialog.pack();
					Point loc = getJFrame().getLocation();
					loc.translate(20, 20);
					aboutDialog.setLocation(loc);
					aboutDialog.setVisible(true);
				}
			});
		}
		return aboutMenuItem;
	}

	/**
	 * This method initializes aboutDialog	
	 * 	
	 * @return javax.swing.JDialog
	 */
	private JDialog getAboutDialog() {
		if (aboutDialog == null) {
			aboutDialog = new JDialog(getJFrame(), true);
			aboutDialog.setTitle("About");
			aboutDialog.setContentPane(getAboutContentPane());
		}
		return aboutDialog;
	}

	/**
	 * This method initializes aboutContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getAboutContentPane() {
		if (aboutContentPane == null) {
			aboutContentPane = new JPanel();
			aboutContentPane.setLayout(new BorderLayout());
			aboutContentPane.add(getAboutVersionLabel(), BorderLayout.CENTER);
		}
		return aboutContentPane;
	}

	/**
	 * This method initializes aboutVersionLabel	
	 * 	
	 * @return javax.swing.JLabel	
	 */
	private JLabel getAboutVersionLabel() {
		if (aboutVersionLabel == null) {
			aboutVersionLabel = new JLabel();
			aboutVersionLabel.setText("UICDS Gui Client Version 1.0");
			aboutVersionLabel.setHorizontalAlignment(SwingConstants.CENTER);
		}
		return aboutVersionLabel;
	}

	/**
	 * This method initializes jMenuItem	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getCutMenuItem() {
		if (cutMenuItem == null) {
			cutMenuItem = new JMenuItem();
			cutMenuItem.setText("Cut");
			cutMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X,
					Event.CTRL_MASK, true));
		}
		return cutMenuItem;
	}

	/**
	 * This method initializes jMenuItem	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getSaveMenuItem() {
		if (saveMenuItem == null) {
			saveMenuItem = new JMenuItem();
			saveMenuItem.setText("Save");
			saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
					Event.CTRL_MASK, true));
		}
		return saveMenuItem;
	}
		
    /*
     * action performed by the buttons
     */
	public void actionPerformed(ActionEvent e) {

		//Handle create button action.
        if (e.getSource() == createButton) {
            boolean accepted = caSubmitter.createAlert(alertSelected);
            ((DefaultTableModel)alertTableModel).fireTableDataChanged();
            if (accepted == true) {
                
                // refresh the incidents list
                incidents = caSubmitter.getListOfAlertIncidents();
                Object [] incidentsArray = incidents.toArray();
                incidentList.setListData(incidentsArray);
                incidentList.clearSelection();
                associateButton.setEnabled(false);

                // set the status bar
                statusText.setText("Incident created.");
            }

        // handle the associate button  
        } else if (e.getSource() == associateButton) {
            boolean accepted = caSubmitter
                .associateAlert(alertSelected, incidentSelected);

            incidentList.clearSelection();
            associateButton.setEnabled(false);
            if (accepted == true) {
                statusText.setText("Alert is associated with Incident.");
            }

        // handle the refresh alerts button  
        } else if (e.getSource() == alertRefreshButton) {
            dataValues = caSubmitter.getListOfAlerts();
            ((DefaultTableModel)alertTableModel)
                .setDataVector(dataValues,columnNames);

            ((DefaultTableModel)alertTableModel)
                .fireTableDataChanged();

            statusText.setText("");

        // handle the refresh incidents button  
        } else if (e.getSource() == incidentRefreshButton) {
            incidents = caSubmitter.getListOfAlertIncidents();
            Object [] incidentsArray = incidents.toArray();
            incidentList.setListData(incidentsArray);
            incidentList.clearSelection();
            associateButton.setEnabled(false);
            statusText.setText("");
        }
    }

    /**
     * listener for the alert table
     */
    private class AlertRowListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent event) {
            if (event.getValueIsAdjusting()) {
                return;
            }

            // if an alert is selected, then get the alert id and save to alertSelected.
            int index = table.getSelectedRow();
            if (index != -1)
            {
                System.out.println("Table Selected Row="+index);
                alertSelected = (String) table.getModel().getValueAt(index, 1);
                System.out.println("Alert Selected:"+alertSelected);
                createButton.setEnabled(true);

            }
            else
            {
                // no alerted is selected: disable create and associate buttons.
                System.out.println("Table Selected Row="+table.getSelectedRow());
                createButton.setEnabled(false);
                associateButton.setEnabled(false);
            }
        }
    }

    /* listener for the incident list
    */
    private class IncidentRowListener implements ListSelectionListener
    {
        public void valueChanged(ListSelectionEvent event) {
            // if the main panel is adjusting, return
            if (event.getValueIsAdjusting()) {
                return;
            }

            // if an incident is selected, and an alert is selected, then enable the associate button.
            if (incidentList.getSelectedIndex() != -1)
            {
                System.out.println("List Selected Row="+ incidentList.getSelectedIndex());
                incidentSelected = (String) incidentList.getSelectedValue();
                System.out.println("List Incident ID Selected:"+incidentSelected);
                if (table.getSelectedRow() != -1) {
                    associateButton.setEnabled(true);
                }

                // if there is no incident selected, then disable the associate button.
                if (table.getSelectedRow() == -1) {
                    associateButton.setEnabled(false);
                }
                
            }
        }
    }

}
