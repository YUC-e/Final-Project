import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;

/**
 * ChessController acts as the bridge between the Model and the View.
 * It listens for user actions from the View, updates the Model accordingly,
 * and instructs the View to refresh based on changes in the Model.
 */
public class ChessController {

    private ChessModel model;
    private ChessView view;

    public ChessController(ChessModel model, ChessView view) {
        this.model = model;
        this.view = view;
        
        // Setup listeners or connect view and model here
    }

    public static void main(String[] args) {
        // Ensure GUI creation is done on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            // Instantiate the components
            ChessModel model = new ChessModel();
            ChessView view = new ChessView(model);
            ChessController controller = new ChessController(model, view);

            // Setup the main window (JFrame)
            JFrame frame = new JFrame("Chess Game");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            // Add the view (JPanel) to the frame
            frame.add(view);
            
            // Set an initial size so it opens a visible blank window
            frame.setPreferredSize(new Dimension(800, 800));
            frame.pack();
            frame.setLocationRelativeTo(null); // Center on screen
            frame.setVisible(true);
        });
    }
}
