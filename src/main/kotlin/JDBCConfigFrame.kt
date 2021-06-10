import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.mysql.jdbc.Driver
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.ActionListener
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import javax.swing.*


class JDBCConfigFrame(private val config: JDBCConfig?) : JFrame(config?.name) {

    //    private val lbConnectionStatus = JLabel()
    private val jtConnectionName = getTextField()
    private val jtHostname = getTextField()
    private val jtUsername = getTextField()
    private val jtPassword = getPasswordField()
    private val jtSchema = getComboBox()
    private val jtLanguage = getComboBox()
    private val jpTablePanel = JPanel()
    private val jList = JList<String>()
    private val btConnect = JButton("connect")
    private val jtPackageName = getTextField()
    private val jtExportPath = getTextField()
//    private val conn: Connection by lazy {
//        getConnection()
//    }

    val jsp = JScrollPane()

    init {
        val jlConnectionName = getLabel("Connection name:")
        val jlHostName = getLabel("Hostname:")
        val jlUserName = getLabel("Username:")
        val jlPassword = getLabel("Password:")

        val jpConnectionName = getPanel()
        jpConnectionName.add(jlConnectionName)
        jpConnectionName.add(jtConnectionName)

        val jpHostName = getPanel()
        jpHostName.add(jlHostName)
        jpHostName.add(jtHostname)

        val jpUserName = getPanel()
        jpUserName.add(jlUserName)
        jpUserName.add(jtUsername)

        val jpPassword = getPanel()
        jpPassword.add(jlPassword)
        jpPassword.add(jtPassword)

        val jlSchema = getLabel("Schema:")
        val jpSchema = getPanel()
        jpSchema.add(jlSchema)
        jpSchema.add(jtSchema)

        val jlLanguage = getLabel("Language:")
        val jpLanguage = getPanel()
        jpLanguage.add(jlLanguage)
        jpLanguage.add(jtLanguage)
        jtLanguage.addItem("java")
        jtLanguage.addItem("kotlin")

        val jpPackage = getPanel()
        val jlPackage = getLabel("Package:")
        jpPackage.add(jlPackage)
        jpPackage.add(jtPackageName)

        val jpExportPath = getPanel()
        val jlExportPath = getLabel("Export path:")
        jpExportPath.add(jlExportPath)
        jpExportPath.add(jtExportPath)

        jtSchema.addItemListener(schemaSelectListener())

        val jpOperation = getPanel()

        btConnect.addActionListener(connectActionListener())

        val btSavaConfig = JButton("save")
        btSavaConfig.addActionListener(saveActionListener())

        jpOperation.add(btConnect)
        jpOperation.add(btSavaConfig)

        if (config != null) {
            val btDelete = JButton("delete")
            jpOperation.add(btDelete)
            btDelete.addActionListener(deleteActionListener())
        }

        val btExport = JButton("export")
        btExport.addActionListener(exportActionListener())
        jpOperation.add(btExport)
//        jpOperation.add(lbConnectionStatus)

        layout = FlowLayout(FlowLayout.LEFT, 0, 10)
        add(jpConnectionName)
        add(jpHostName)
        add(jpUserName)
        add(jpPassword)
        add(jpSchema)
        add(jpLanguage)
        add(jpPackage)
        add(jpExportPath)
        add(jpOperation)

        jpTablePanel.preferredSize = Dimension(600, 200)
        jpTablePanel.layout = FlowLayout(FlowLayout.LEFT, 0, 0)

        jList.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
        jsp.setViewportView(jList)
        add(jsp)
        jsp.preferredSize = Dimension(570, 280)
        jsp.isVisible = false

        /*val jpExport = getPanel()
        jpExport.add(btExport)
        add(jpExport)*/

        if (config != null) {
            jtConnectionName.text = config.name
            jtHostname.text = config.host
            jtUsername.text = config.username
            jtPassword.text = config.password
            jtPackageName.text = config.packagename
            jtExportPath.text = config.exportpath
            jtLanguage.selectedItem = config.language
        }

        config?.let {
            connectAction()
        }
    }

    private fun exportActionListener() = ActionListener {
        jList.selectedValuesList.forEach {
            val entityArray = arrayListOf<EntityModel>()
            val conn = getConnection()
            val md = conn.metaData
            var primaryKey = ""
            val primaryKeyResultSet = md.getPrimaryKeys(jtSchema.selectedItem.toString(), null, it)
            while (primaryKeyResultSet.next()) {
                primaryKey = primaryKeyResultSet.getString("COLUMN_NAME")
            }

            val col = md.getColumns(jtSchema.selectedItem.toString(), "%", it, "%")
            while (col.next()) {
                val columnName = col.getString("COLUMN_NAME");
                val typeName = col.getString("TYPE_NAME")
                println(typeName)
                val entity = EntityModel(columnName, typeName, primaryKey.equals(columnName))
                entityArray.add(entity)
            }
            val exporter = EntityExporter(
                jtExportPath.text,
                jtConnectionName.text,
                jtPackageName.text,
                jtSchema.selectedItem.toString(),
                it,
                jtLanguage.selectedItem.toString(),
                entityArray
            )
            exporter.export()
        }
    }

    private fun schemaSelectListener() = ItemListener {
        if (it.stateChange == ItemEvent.SELECTED) {
            val stmt = getConnection().createStatement()
            stmt.execute("use `${it.item}`")
            val rs = stmt.executeQuery("show tables")
            val rsMetaData = rs.metaData
            val tableList = arrayListOf<String>()
            while (rs.next()) {
                val count: Int = rsMetaData.columnCount
                for (i in 0 until count) {
                    tableList.add(rs.getString(i + 1))
                }
            }
            generateTableList(tableList)
        }
    }

    private fun generateTableCheckBox(tableList: ArrayList<String>) {
        jpTablePanel.removeAll()
        tableList.forEach {
            val checkbox = JCheckBox(it)
            jpTablePanel.add(checkbox)
        }
        jpTablePanel.validate()
        jpTablePanel.repaint()
    }

    private fun generateTableList(tableList: ArrayList<String>) {
        jsp.isVisible = true
        val height = 17 * tableList.size
        jList.preferredSize = Dimension(550, height)
        var data = Array(tableList.size) { "" }
        println(tableList.size)
        for (i in 0 until tableList.size) {
            data[i] = tableList[i]
//            println(tableList[i])
        }
        jList.setListData(data)
    }

    private fun saveActionListener() = ActionListener {
        saveConfig()
    }

    private fun connectActionListener() = ActionListener {
        connectAction()
    }

    private fun connectAction() {
        try {
            val stmt = getConnection().createStatement()
            val rs = stmt.executeQuery("show databases")
            val rsMetaData = rs.metaData
            jtSchema.removeAllItems()
            while (rs.next()) {
                val count: Int = rsMetaData.columnCount
                for (i in 0 until count) {
                    jtSchema.addItem(rs.getString(i + 1))
                }
            }
            btConnect.isVisible = false
            jtSchema.selectedItem = config?.schema
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, e.message, "connect failure", JOptionPane.ERROR_MESSAGE)
        }
    }

    private fun deleteActionListener() = ActionListener {
        val array = fileConfigArray()
        var selectObj: JSONObject? = null
        array.forEach {
            if (it is JSONObject) {
                if (it.getString("sid").equals(config?.sid)) {
                    selectObj = it
                }
            }
        }
        selectObj?.let {
            array.remove(selectObj)
        }
        saveFile(filename, array.toString())
        dispose()
    }

    /**
     * 保存配置
     */
    private fun saveConfig() {
        println(File(filename).absolutePath)
        //修改
        if (config != null) {
            val array = fileConfigArray()
            val newArray = JSONArray()
            array.forEach {
                if (it is JSONObject) {
                    if (it.getString("sid").equals(config.sid)) {
                        it["sid"] = config.sid
                        it["name"] = jtConnectionName.text
                        it["host"] = jtHostname.text
                        it["username"] = jtUsername.text
                        it["password"] = jtPassword.text
                        it["package"] = jtPackageName.text
                        it["schema"] = jtSchema.selectedItem.toString()
                        it["exportpath"] = jtExportPath.text
                        it["language"] = jtLanguage.selectedItem
                    }
                    newArray.add(it)
                }
            }
            saveFile(filename, newArray.toString())
        }
        //新增
        else {
            val item = JSONObject()
            item["sid"] = UUID.randomUUID().toString()
            item["name"] = jtConnectionName.text
            item["host"] = jtHostname.text
            item["username"] = jtUsername.text
            item["password"] = jtPassword.text
            item["package"] = jtPackageName.text
            item["exportpath"] = jtExportPath.text
            item["schema"] = jtSchema.selectedItem ?: ""
            item["language"] = jtLanguage.selectedItem
            var array = JSONArray()

            val file = File(filename)
            if (file.exists()) {
                array = fileConfigArray()
            }
            array.add(item)
            saveFile(filename, array.toString())
        }
        JOptionPane.showMessageDialog(null, "config saved")
//        JOptionPane.showMessageDialog(this, "config saved", "message", JOptionPane.NO_OPTION)
    }

    private fun fileConfigArray(): JSONArray {
        val path: Path = Paths.get(filename)
        val data = Files.readAllBytes(path)
        val result = String(data)
        return JSON.parseArray(result)
    }

    private fun getLabel(text: String): JLabel {
        val jLabel = JLabel(text)
        jLabel.preferredSize = Dimension(120, 20)
        return jLabel
    }

    private fun getPasswordField(): JTextField {
        val textField = JPasswordField()
        textField.preferredSize = Dimension(400, 20)
        return textField
    }

    private fun getTextField(): JTextField {
        val textField = JTextField()
        textField.preferredSize = Dimension(400, 20)
        return textField
    }

    private fun getComboBox(): JComboBox<String> {
        val c = JComboBox<String>()
        c.preferredSize = Dimension(400, 20)
        return c
    }

    private fun getPanel(): JPanel {
        val jp = JPanel()
        jp.preferredSize = Dimension(600, 30)
        return jp
    }

    private fun getConnection(): Connection {
        println("connect")
        val host = jtHostname.text
        val username = jtUsername.text
        val password = jtPassword.text
        return getConnection(host, username, password)
    }

    private fun getConnection(host: String, user: String, password: String): Connection {
        try {
            val url = "jdbc:mysql://${host}"
            Class.forName(Driver::class.java.name)
            return DriverManager.getConnection(url, user, password)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

}