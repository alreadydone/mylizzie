package featurecat.lizzie.gui;

import com.jhlabs.image.GaussianFilter;
import featurecat.lizzie.Lizzie;
import featurecat.lizzie.rules.*;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The window used to display the game.
 */
public class LizzieFrame extends JFrame {
    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle("featurecat.lizzie.i18n.GuiBundle");

    private static final String[] commands = {
            resourceBundle.getString("LizzieFrame.controls.leftArrow")
            , resourceBundle.getString("LizzieFrame.controls.rightArrow")
            , resourceBundle.getString("LizzieFrame.controls.space")
            , resourceBundle.getString("LizzieFrame.controls.rightClick")
            , resourceBundle.getString("LizzieFrame.controls.mouseWheelScroll")
            , resourceBundle.getString("LizzieFrame.controls.keyP")
            , resourceBundle.getString("LizzieFrame.controls.keyN")
            , resourceBundle.getString("LizzieFrame.controls.keyO")
            , resourceBundle.getString("LizzieFrame.controls.keyAltC")
            , resourceBundle.getString("LizzieFrame.controls.keyCtrlO")
            , resourceBundle.getString("LizzieFrame.controls.keyCtrlS")
            , resourceBundle.getString("LizzieFrame.controls.keyCtrlC")
            , resourceBundle.getString("LizzieFrame.controls.keyCtrlV")
            , resourceBundle.getString("LizzieFrame.controls.keyG")
            , resourceBundle.getString("LizzieFrame.controls.keyV")
            , resourceBundle.getString("LizzieFrame.controls.keyX")
            , resourceBundle.getString("LizzieFrame.controls.keyA")
            , resourceBundle.getString("LizzieFrame.controls.keyH")
            , resourceBundle.getString("LizzieFrame.controls.keyHome")
            , resourceBundle.getString("LizzieFrame.controls.keyEnd")
            , resourceBundle.getString("LizzieFrame.controls.keyS")
            , resourceBundle.getString("LizzieFrame.controls.keyC")
            , resourceBundle.getString("LizzieFrame.controls.keyE")
            , resourceBundle.getString("LizzieFrame.controls.keyB")
            , resourceBundle.getString("LizzieFrame.controls.keyEnter")
            , resourceBundle.getString("LizzieFrame.controls.keyT")
            , resourceBundle.getString("LizzieFrame.controls.keyI")
    };
    public static final String LIZZIE_TITLE = String.format("MyLizzie %s", StringUtils.defaultString(Lizzie.getLizzieVersion(), "dev-edition"));
    public static final String LIZZIE_TRY_PLAY_TITLE = resourceBundle.getString("LizzieFrame.title.tryPlayingMode");

    static {
        // load fonts
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, LizzieFrame.class.getResourceAsStream("/fonts/OpenSans-Regular.ttf")));
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, LizzieFrame.class.getResourceAsStream("/fonts/OpenSans-Semibold.ttf")));
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
        }
    }

    private String engineProfile = Lizzie.optionSetting.getLeelazCommandLine();

    private BufferedImage cachedImage;
    private BoardRenderer boardRenderer;

    private JMenuBar menuBar;
    private JPanel mainPanel;
    private JLabel statusBar;
    private final BoardStateChangeObserver boardStateChangeObserver;
    private final GameInfo.GameInfoChangeListener gameInfoChangeListener;

    public boolean showControls = false;

    // TODO: Clean this
    public boolean isPlayingAgainstLeelaz = false;

    /**
     * Creates a window and refreshes the game state at FPS.
     */
    public LizzieFrame() {
        super();
        setTitle(LIZZIE_TITLE + " - [" + engineProfile + "]");

        Input input = new Input();
        initMenu(input);

        boardRenderer = new BoardRenderer();
        mainPanel = new JPanel(true) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                paintBoardAndBackground(g);
            }
        };
        mainPanel.addMouseMotionListener(input);
        mainPanel.addMouseListener(input);
        mainPanel.addMouseWheelListener(input);

        addKeyListener(input);

        setAlwaysOnTop(Lizzie.optionSetting.isMainWindowAlwaysOnTop());
        getContentPane().add(mainPanel, BorderLayout.CENTER);

        statusBar = new JLabel();
        updateStatusBar(Stone.BLACK, 0, 0, Lizzie.gameInfo.getKomi());
        boardStateChangeObserver = new BoardStateChangeObserver() {
            @Override
            public void mainStreamAppended(BoardHistoryNode newNodeBegin, BoardHistoryNode head) {
            }

            @Override
            public void mainStreamCut(BoardHistoryNode nodeBeforeCutPoint, BoardHistoryNode head) {
            }

            @Override
            public void headMoved(BoardHistoryNode oldHead, BoardHistoryNode newHead) {
                SwingUtilities.invokeLater(() -> updateStatusBar(
                        newHead.getData().getLastMoveColor().opposite()
                        , newHead.getData().getBlackPrisonersCount()
                        , newHead.getData().getWhitePrisonersCount()
                        , Lizzie.gameInfo.getKomi()
                ));
            }

            @Override
            public void boardCleared(BoardHistoryNode initialNode, BoardHistoryNode initialHead) {
                SwingUtilities.invokeLater(() -> updateStatusBar(Stone.BLACK, 0, 0, Lizzie.gameInfo.getKomi()));
            }
        };
        gameInfoChangeListener = (info, changedItemTypes) -> {
            if (changedItemTypes.contains(GameInfo.StateChangedItemType.KOMI)) {
                SwingUtilities.invokeLater(() -> updateStatusBar(
                        Lizzie.board.getData().getLastMoveColor().opposite()
                        , Lizzie.board.getData().getBlackPrisonersCount()
                        , Lizzie.board.getData().getWhitePrisonersCount()
                        , info.getKomi()
                ));
            }
        };

        Lizzie.board.registerBoardStateChangeObserver(boardStateChangeObserver);
        Lizzie.gameInfo.registerGameInfoChangeListener(gameInfoChangeListener);
        getContentPane().add(statusBar, BorderLayout.SOUTH);

        // shut down leelaz, then shut down the program when the window is closed
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                Lizzie.gameInfo.unregisterGameInfoChangeListener(gameInfoChangeListener);
                Lizzie.board.unregisterBoardStateChangeObserver(boardStateChangeObserver);

                Lizzie.readGuiPosition();
                Lizzie.writeSettingFile();

                Lizzie.notifyExitLizzie(0);
            }
        });

        setVisible(true);
    }

    private void initMenu(final Input input) {
        JMenu menu;
        JMenuItem item;

        menuBar = new JMenuBar();

        menu = new JMenu(resourceBundle.getString("LizzieFrame.menu.file"));
        menuBar.add(menu);

        item = new JMenuItem(resourceBundle.getString("LizzieFrame.menu.file.open"));
        item.addActionListener(e -> {
            Lizzie.board.leaveTryPlayState();
            Lizzie.loadGameByPrompting();
        });
        menu.add(item);

        item = new JMenuItem(resourceBundle.getString("LizzieFrame.menu.file.save"));
        item.addActionListener(e -> {
            Lizzie.board.leaveTryPlayState();
            Lizzie.storeGameByPrompting();
        });
        menu.add(item);

        menu = new JMenu(resourceBundle.getString("LizzieFrame.menu.edit"));
        menuBar.add(menu);

        item = new JMenuItem(resourceBundle.getString("LizzieFrame.menu.edit.copy"));
        item.addActionListener(e -> {
            Lizzie.board.leaveTryPlayState();
            Lizzie.copyGameToClipboardInSgf();
        });
        menu.add(item);

        item = new JMenuItem(resourceBundle.getString("LizzieFrame.menu.edit.paste"));
        item.addActionListener(e -> {
            Lizzie.board.leaveTryPlayState();
            Lizzie.pasteGameFromClipboardInSgf();
        });
        menu.add(item);

        item = new JMenuItem(resourceBundle.getString("LizzieFrame.menu.edit.modifyMove"));
        item.addActionListener(e -> {
            Lizzie.promptForChangeExistingMove();
        });
        menu.add(item);

        menu = new JMenu(resourceBundle.getString("LizzieFrame.menu.navigate"));
        menuBar.add(menu);

        item = new JMenuItem(resourceBundle.getString("LizzieFrame.menu.navigate.toBegin"));
        item.addActionListener(e -> {
            Lizzie.board.gotoMove(0);
        });
        menu.add(item);

        item = new JMenuItem(resourceBundle.getString("LizzieFrame.menu.navigate.toEnd"));
        item.addActionListener(e -> {
            Lizzie.board.gotoMove(Integer.MAX_VALUE);
        });
        menu.add(item);

        item = new JMenuItem(resourceBundle.getString("LizzieFrame.menu.navigate.gotoMove"));
        item.addActionListener(e -> {
            input.promptForGotoMove();
        });
        menu.add(item);

        item = new JMenuItem(resourceBundle.getString("LizzieFrame.menu.navigate.setGameInfo"));
        item.addActionListener(e -> {
            JDialog dialog = new GameInfoDialog(Lizzie.frame);
            dialog.setVisible(true);
        });
        menu.add(item);

        menu = new JMenu(resourceBundle.getString("LizzieFrame.menu.game"));
        menuBar.add(menu);

        item = new JMenuItem(resourceBundle.getString("LizzieFrame.menu.game.pass"));
        item.addActionListener(e -> {
            Lizzie.board.pass();
        });
        menu.add(item);

        item = new JMenuItem(resourceBundle.getString("LizzieFrame.menu.game.scoring"));
        item.addActionListener(e -> {
            input.scoreGame();
        });
        menu.add(item);

        item = new JMenuItem(resourceBundle.getString("LizzieFrame.menu.game.tryPlayingMode"));
        item.addActionListener(e -> {
            if (Lizzie.board.isInTryPlayState()) {
                Lizzie.board.leaveTryPlayState();
            } else {
                Lizzie.board.enterTryPlayState();
            }
        });
        menu.add(item);

        menu = new JMenu(resourceBundle.getString("LizzieFrame.menu.help"));
        menuBar.add(menu);

        item = new JMenuItem(resourceBundle.getString("LizzieFrame.menu.help.about"));
        item.addActionListener(e -> {
            AboutDialog aboutDialog = new AboutDialog(this);
            aboutDialog.setVisible(true);
        });
        menu.add(item);

        setJMenuBar(menuBar);
    }

    public BoardRenderer getBoardRenderer() {
        return boardRenderer;
    }

    public BufferedImage getCachedImage() {
        return cachedImage;
    }

    // Toggle show/hide move number
    public void toggleShowMoveNumber() {
        Lizzie.optionSetting.setShowMoveNumber(!Lizzie.optionSetting.isShowMoveNumber());
    }

    public void setEngineProfile(String engineProfile) {
        this.engineProfile = engineProfile;
        if (getTitle().startsWith(LIZZIE_TITLE)) {
            setTitle(LIZZIE_TITLE + " - [" + engineProfile + "]");
        }
    }

    /**
     * Draws the game board and interface
     *
     * @param g0 not used
     */
    public void paintBoardAndBackground(Graphics g0) {
        // initialize
        final int width = mainPanel.getWidth();
        final int height = mainPanel.getHeight();
        cachedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) cachedImage.getGraphics();

        int topInset = mainPanel.getInsets().top;

        if (Lizzie.optionSetting.isShowFancyBoard()) {
            try {
                BufferedImage background = AssetsManager.getAssetsManager().getImageAsset("assets/background.jpg");
                int drawWidth = Math.max(background.getWidth(), width);
                int drawHeight = Math.max(background.getHeight(), height);
                g.drawImage(background, 0, 0, drawWidth, drawHeight, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            g.setColor(Color.GREEN.darker().darker());
            g.fillRect(0, 0, width, height);
        }

        int maxSize = Math.max(Math.min(width, height - topInset), Board.BOARD_SIZE + 5); // don't let maxWidth become too small

        drawCommandString(g);

        int boardX = (width - maxSize) / 2;
        int boardY = topInset + (height - topInset - maxSize) / 2;
        boardRenderer.setLocation(boardX, boardY);
        boardRenderer.setBoardLength(maxSize);
        boardRenderer.draw(g);

        // cleanup
        g.dispose();

        // draw the control hint
        if (showControls) {
            drawControls();
        }

        // draw the image
        g0.drawImage(cachedImage, 0, 0, null);
    }

    private GaussianFilter filter = new GaussianFilter(15);

    /**
     * Display the controls
     */
    private void drawControls() {
        userAlreadyKnowsAboutCommandString = true;

        Graphics2D g = (Graphics2D) cachedImage.getGraphics();
        int width = mainPanel.getWidth();
        int height = mainPanel.getHeight();
        int maxSize = Math.min(width, height);

        Font font = new Font("SimSun", Font.PLAIN, (int) (maxSize * 0.03)); //jLabel font
        g.setFont(font);
        int lineHeight = (int) (font.getSize() * 1.15);

        int boxWidth = (int) (maxSize * 0.85);
        int boxHeight = (int) (commands.length * lineHeight);

        int commandsX = (int) (width / 2 - boxWidth / 2);
        int commandsY = (int) (height / 2 - boxHeight / 2);

        BufferedImage result = new BufferedImage(boxWidth, boxHeight, BufferedImage.TYPE_INT_ARGB);
        filter.filter(cachedImage.getSubimage(commandsX, commandsY, boxWidth, boxHeight), result);
        g.drawImage(result, commandsX, commandsY, null);

        g.setColor(new Color(0, 0, 0, 130));
        g.fillRect(commandsX, commandsY, boxWidth, boxHeight);
        int strokeRadius = 2;
        g.setStroke(new BasicStroke(2 * strokeRadius));
        g.setColor(new Color(0, 0, 0, 60));
        g.drawRect(commandsX + strokeRadius, commandsY + strokeRadius, boxWidth - 2 * strokeRadius, boxHeight - 2 * strokeRadius);

        int verticalLineX = (int) (commandsX + boxWidth * 0.3);
        g.setColor(new Color(0, 0, 0, 60));
        g.drawLine(verticalLineX, commandsY + 2 * strokeRadius, verticalLineX, commandsY + boxHeight - 2 * strokeRadius);


        g.setStroke(new BasicStroke(1));

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        FontMetrics metrics = g.getFontMetrics(font);

        g.setColor(Color.WHITE);
        for (int i = 0; i < commands.length; i++) {
            String[] split = commands[i].split("\\|");
            g.drawString(split[0], verticalLineX - metrics.stringWidth(split[0]) - strokeRadius * 4, font.getSize() + (int) (commandsY + i * lineHeight));
            g.drawString(split[1], verticalLineX + strokeRadius * 4, font.getSize() + (int) (commandsY + i * lineHeight));
        }
    }

    private boolean userAlreadyKnowsAboutCommandString = false;

    private void drawCommandString(Graphics2D g) {
        if (userAlreadyKnowsAboutCommandString)
            return;

        int width = mainPanel.getWidth();
        int height = mainPanel.getHeight();
        int maxSize = (int) (Math.min(width, height) * 0.98);

        Font font = new Font("SimSun", Font.PLAIN, (int) (maxSize * 0.03));
        String commandString = resourceBundle.getString("LizzieFrame.controls.keyF1");
        int strokeRadius = 2;

        int showCommandsHeight = (int) (font.getSize() * 1.1);
        int showCommandsWidth = g.getFontMetrics(font).stringWidth(commandString) + 4 * strokeRadius;
        int showCommandsX = mainPanel.getInsets().left;
        int showCommandsY = height - showCommandsHeight - mainPanel.getInsets().bottom;
        g.setColor(new Color(0, 0, 0, 130));
        g.fillRect(showCommandsX, showCommandsY, showCommandsWidth, showCommandsHeight);
        g.setStroke(new BasicStroke(2 * strokeRadius));
        g.setColor(new Color(0, 0, 0, 60));
        g.drawRect(showCommandsX + strokeRadius, showCommandsY + strokeRadius, showCommandsWidth - 2 * strokeRadius, showCommandsHeight - 2 * strokeRadius);
        g.setStroke(new BasicStroke(1));

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.setFont(font);
        g.drawString(commandString, showCommandsX + 2 * strokeRadius, showCommandsY + font.getSize());
    }

    /**
     * Checks whether or not something was clicked and performs the appropriate action
     *
     * @param x x coordinate
     * @param y y coordinate
     */
    public void onClicked(int x, int y) {
        // check for board click
        int[] boardCoordinates = boardRenderer.convertScreenToCoordinates(x, y);

        if (boardCoordinates != null) {
            Lizzie.board.place(boardCoordinates[0], boardCoordinates[1]);
        }
    }

    private AtomicReference<int[]> lastBoardCoordinates = new AtomicReference<>();

    public void onMouseMove(int x, int y) {
        if (Lizzie.optionSetting.isMouseOverShowMove() && (Lizzie.board.getData().isBlackToPlay() && Lizzie.optionSetting.isShowBlackSuggestion()
                || !Lizzie.board.getData().isBlackToPlay() && Lizzie.optionSetting.isShowWhiteSuggestion())) {
            // check for board click
            int[] boardCoordinates = boardRenderer.convertScreenToCoordinates(x, y);
            int[] previousCoordinates = lastBoardCoordinates.getAndSet(boardCoordinates);
            if (!Arrays.equals(previousCoordinates, boardCoordinates)) {
                Lizzie.analysisFrame.getAnalysisTableModel().selectOrDeselectMoveByCoord(boardCoordinates);
                repaint();
            }
        }
    }

    public void onDoubleClicked(int x, int y) {
        int[] boardCoordinates = boardRenderer.convertScreenToCoordinates(x, y);

        if (boardCoordinates != null) {
            int moveNumber = Lizzie.board.getMoveNumber(boardCoordinates[0], boardCoordinates[1]);
            if (moveNumber > 0) {
                Lizzie.board.gotoMove(moveNumber);
            }
        }
    }

    public void showTryPlayTitle() {
        setTitle(LIZZIE_TRY_PLAY_TITLE);
    }

    public void restoreDefaultTitle() {
        setTitle(LIZZIE_TITLE + " - [" + engineProfile + "]");
    }

    public void updateStatusBar(Stone nextMove, int blackPrisoners, int whitePrisoners, double komi) {
        String statusBarText = String.format(resourceBundle.getString("LizzieFrame.statusBar.formatter")
                , komi
                , formatColor(nextMove)
                , blackPrisoners
                , whitePrisoners
        );

        statusBar.setText(statusBarText);
    }

    private String formatColor(Stone stone) {
        switch (stone) {
            case BLACK:
            case BLACK_GHOST:
            case BLACK_RECURSED:
                return resourceBundle.getString("LizzieFrame.prompt.colorBlack");
            case WHITE:
            case WHITE_GHOST:
            case WHITE_RECURSED:
                return resourceBundle.getString("LizzieFrame.prompt.colorWhite");
            default:
                return "?";
        }
    }
}
