import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import java.awt.FlowLayout
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.*

val filename = "config.json"

fun main() {
    var jf = JFrame("JPAEntityGenerator")
    jf.setSize(532, 500)

    val menubar = JMenuBar()
    val menuFile = JMenu("File")
    val itemNew = JMenuItem("New")
    itemNew.addActionListener {
        showConfigFrame(null)
    }

    val itemRefresh = JMenuItem("refresh")
    itemRefresh.addActionListener {
        refreshData(jf)
    }
    menuFile.add(itemNew)
    menuFile.add(itemRefresh)
    menubar.add(menuFile)
    jf.jMenuBar = menubar

    jf.addWindowFocusListener(object : WindowFocusListener {
        override fun windowGainedFocus(e: WindowEvent?) {
            refreshData(jf)
        }

        override fun windowLostFocus(e: WindowEvent?) {
        }
    })

    jf.layout = FlowLayout(FlowLayout.LEFT)
    jf.setLocationRelativeTo(null);//在屏幕中居中显示
    jf.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    jf.isVisible = true
}

inline fun refreshData(jf: JFrame) {
    jf.contentPane.removeAll()
    val btList = loadConfig()
    btList.forEach {
        jf.add(it)
    }
    jf.validate()
    jf.repaint()
}

fun showConfigFrame(jdbcConfig: JDBCConfig?) {
    val jdbcConfigFrame = JDBCConfigFrame(jdbcConfig)
    jdbcConfigFrame.setSize(569, 650)
    jdbcConfigFrame.setLocationRelativeTo(null);//在屏幕中居中显示
    jdbcConfigFrame.isVisible = true
}

fun loadConfig(): List<JButton> {
    val buttonList = arrayListOf<JButton>()
    if (File(filename).exists()) {
        val path: Path = Paths.get(filename)
        val data = Files.readAllBytes(path)
        val result = String(data)
        val configArr = JSON.parseArray(result)

        if (configArr != null && configArr.size > 0) {
            configArr.forEach {
                if (it is JSONObject) {
                    val title = it.getString("name")
                    val packageName = it.getString("package") ?: ""
                    val schema = it.getString("schema") ?: ""
                    val exportpath = it.getString("exportpath") ?: ""
                    val language = it.getString("language") ?: ""
                    val jdbcConfig =
                        JDBCConfig(
                            it.getString("sid"),
                            title,
                            it.getString("host"),
                            it.getString("username"),
                            it.getString("password"),
                            packageName,
                            schema,
                            exportpath,
                            language
                        )
                    val mainButton = JEGMainButton(title)
                    mainButton.addActionListener {
                        showConfigFrame(jdbcConfig)
                    }
                    buttonList.add(mainButton)
                }
            }
        }

    }
    return buttonList;
}