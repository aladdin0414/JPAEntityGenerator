import java.awt.Dimension
import javax.swing.JButton

class JEGMainButton(text: String) : JButton(text) {

    init {
        this.preferredSize = Dimension(100, 100)
    }
}