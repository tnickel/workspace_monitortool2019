package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import components.MainTable;
import data.DataManager;
import data.FavoritesManager;
import data.ProviderStats;
import models.FilterCriteria;
import ui.OpenTradesDialog;

public class MainFrame extends JFrame {
   private static final Logger LOGGER = Logger.getLogger(MainFrame.class.getName());
   private final MainTable mainTable;
   private final JLabel statusLabel;
   private final JTextField searchField;
   private final DataManager dataManager;
   private final String rootPath;
   private final FavoritesManager favoritesManager;
   private int[] currentSearchIndex = new int[]{-1};

   public MainFrame(DataManager dataManager, String rootPath) {
       super("Signal Providers Performance Analysis");
       this.dataManager = dataManager;
       this.rootPath = rootPath;
       this.favoritesManager = new FavoritesManager(rootPath);

       // Create components
       mainTable = new MainTable(dataManager, rootPath);
       statusLabel = new JLabel(" ");
       searchField = new JTextField(20);
       
       setupUI();
       setupSearch();
       setupStatusBar();
   }

   private void setupUI() {
       setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
       
       // Main layout
       JPanel contentPane = new JPanel(new BorderLayout(5, 5));
       contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
       setContentPane(contentPane);
       
       // Toolbar
       createToolBar();
       
       // Main table
       JScrollPane scrollPane = new JScrollPane(mainTable);
       contentPane.add(scrollPane, BorderLayout.CENTER);
       
       // Status bar
       JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
       statusBar.add(statusLabel);
       contentPane.add(statusBar, BorderLayout.SOUTH);
       
       // Set initial size
       setSize(1200, 800);
       setLocationRelativeTo(null);
   }

   private void createToolBar() {
       JToolBar toolBar = new JToolBar();
       toolBar.setFloatable(false);
       
       // Search panel
       JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
       searchPanel.add(new JLabel("Search:"));
       searchPanel.add(searchField);
       
       JButton searchButton = new JButton("Search");
       searchButton.addActionListener(e -> performSearch());
       searchPanel.add(searchButton);
       
       toolBar.add(searchPanel);
       toolBar.addSeparator();

       // Favorites Toggle Button
       JToggleButton favoritesToggle = new JToggleButton("Show Favorites");
       favoritesToggle.addActionListener(e -> {
           if (favoritesToggle.isSelected()) {
               Map<String, ProviderStats> favorites = dataManager.getStats().entrySet().stream()
                   .filter(entry -> favoritesManager.isFavorite(
                       entry.getKey().substring(entry.getKey().lastIndexOf("_") + 1).replace(".csv", "")
                   ))
                   .collect(Collectors.toMap(
                       Map.Entry::getKey,
                       Map.Entry::getValue
                   ));
               mainTable.setData(favorites);
           } else {
               mainTable.setData(dataManager.getStats());
           }
       });
       toolBar.add(favoritesToggle);
       toolBar.addSeparator();
       
       // Filter button
       JButton filterButton = new JButton("Filter");
       filterButton.addActionListener(e -> showFilterDialog());
       toolBar.add(filterButton);
       
       // Compare buttons
       JButton compareButton = new JButton("Compare Equity Curves");
       compareButton.addActionListener(e -> showCompareDialog());
       toolBar.add(compareButton);
       
       JButton compareOpenTradesButton = new JButton("Compare Open Trades");
       compareOpenTradesButton.addActionListener(e -> {
           OpenTradesDialog dialog = new OpenTradesDialog(this, mainTable.getCurrentProviderStats(), rootPath);
           dialog.setVisible(true);
       });
       toolBar.add(compareOpenTradesButton);
       
       add(toolBar, BorderLayout.NORTH);
   }

   private void setupSearch() {
       searchField.addActionListener(e -> performSearch());
       
       searchField.addKeyListener(new KeyAdapter() {
           @Override
           public void keyPressed(KeyEvent e) {
               if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                   searchField.setText("");
                   mainTable.clearHighlight();
                   currentSearchIndex[0] = -1;
               }
           }
       });
   }

   private void setupStatusBar() {
       statusLabel.setBorder(new EmptyBorder(2, 5, 2, 5));
   }

   private void performSearch() {
       String searchText = searchField.getText().toLowerCase().trim();
       if (searchText.isEmpty()) {
           mainTable.clearHighlight();
           currentSearchIndex[0] = -1;
           return;
       }

       mainTable.highlightSearchText(searchText);
       if (!mainTable.findAndSelectNext(searchText, currentSearchIndex)) {
           currentSearchIndex[0] = -1;
           if (!mainTable.findAndSelectNext(searchText, currentSearchIndex)) {
               JOptionPane.showMessageDialog(this,
                   "No matches found for: " + searchText,
                   "Search Result",
                   JOptionPane.INFORMATION_MESSAGE);
           }
       }
   }

   private void showFilterDialog() {
       FilterDialog dialog = new FilterDialog(this);
       FilterCriteria criteria = dialog.showDialog();
       if (criteria != null) {
           mainTable.applyFilter(criteria);
       }
   }

   private void showCompareDialog() {
       CompareDialog dialog = new CompareDialog(this, mainTable.getCurrentProviderStats(), rootPath);
       dialog.setVisible(true);
   }

   public void display() {
       SwingUtilities.invokeLater(() -> {
           setVisible(true);
           SwingUtilities.invokeLater(() -> {
               mainTable.updateStatus();
           });
       });
   }
}