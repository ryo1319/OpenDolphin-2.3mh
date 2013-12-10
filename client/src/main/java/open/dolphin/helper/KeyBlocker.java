package open.dolphin.helper;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JComponent;

/**
 *
 * @author Kazushi Minagawa.
 */
public final class KeyBlocker implements KeyListener {
    
    private final JComponent target;
    
    public KeyBlocker(JComponent target) {
        this.target = target;
    }
    
    public void block() {
        target.addKeyListener(this);
    }
    
    public void unblock() {
        target.removeKeyListener(this);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        e.consume();
    }

    /** Handle the key-pressed event from the text field. */
    @Override
    public void keyPressed(KeyEvent e) {
        e.consume();
    }

    /** Handle the key-released event from the text field. */
    @Override
    public void keyReleased(KeyEvent e) {
        e.consume();
    }
}
