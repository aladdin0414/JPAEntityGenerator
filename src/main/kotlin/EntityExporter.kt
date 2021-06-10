import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class EntityExporter(
    private val exportFolderPath: String,
    private val connectionName: String,
    private val packageName: String,
    private val schema: String,
    private val tableName: String,
    private val language: String,
    private val entityArray: ArrayList<EntityModel>
) {

    fun export() {
        val gencode = StringBuffer()
        val javaClassName = word2JavaClassName(tableName)
        val classVarName = firstLetter2LowerCase(javaClassName)
        val format = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
        val dateString: String = format.format(Date())
        val generatorName = "JPAEntityGenerator"
        if (language.equals("kotlin")) {
            gencode.append("package $packageName;")
            gencode.append("\n")
            gencode.append("\nimport java.sql.Timestamp")
            gencode.append("\nimport java.util.*")
            gencode.append("\nimport javax.persistence.*")
            gencode.append("\n")
            gencode.append("\n")
            gencode.append(
                "/**\n" +
                        " * Generated by $generatorName\n" +
                        " * @date $dateString\n" +
                        " */"
            )
            gencode.append("\n@Entity")
            gencode.append("\n@Table(name = \"$tableName\", schema = \"$schema\", catalog = \"\")")
            gencode.append("\nclass $javaClassName {")

            entityArray.forEach {
                gencode.append("\n\n\t@Column(name = \"${it.columnname}\")")
                if (it.isPrimaryKey) {
                    gencode.append("\n\t@Id")
                } else {
                    gencode.append("\n\t@Basic")
                }
                gencode.append("\n\tvar ${columnName2VarName(it.columnname)}: ${getTypeStr(it.typename)}? = null")
            }

            gencode.append("\n\n\toverride fun equals(o: Any?): Boolean {")
            gencode.append("\n\t\tif (this === o) return true")
            gencode.append("\n\t\tif (o == null || javaClass != o.javaClass) return false")
            gencode.append("\n\t\tval $classVarName = o as $javaClassName")
            gencode.append("\n\t\treturn ")
            entityArray.forEachIndexed { id, it ->
                val varName = columnName2VarName(it.columnname)
                if (id == entityArray.size - 1) {
                    gencode.append("$varName == $classVarName.$varName")
                }
                else{
                    gencode.append("$varName == $classVarName.$varName && ")
                }
            }
            gencode.append("\n\t}")
            gencode.append("\n")
            gencode.append("\n\toverride fun hashCode(): Int {")
            gencode.append("\n\t\treturn Objects.hash(")
            entityArray.forEachIndexed { index, entityModel ->
                val varName = columnName2VarName(entityModel.columnname)
                if(index == entityArray.size - 1){
                    gencode.append(varName)
                }else{
                    gencode.append("$varName, ")
                }
            }
            gencode.append(")")
            gencode.append("\n\t}")
            gencode.append("\n}")

        } else {
            gencode.append("package $packageName;")
            gencode.append("\nimport javax.persistence.*;")
            gencode.append("\nimport java.sql.Timestamp;")
            gencode.append("\nimport java.util.Objects;")

            gencode.append("\n")
            gencode.append("\n")

            gencode.append(
                "/**\n" +
                        " * Generated by JPAEntityGenerator\n" +
                        " * @date $dateString\n" +
                        " * \n" +
                        " */"
            )
            gencode.append("\n@Entity")
            gencode.append("\n@Table(name = \"$tableName\", schema = \"$schema\", catalog = \"\")")
            gencode.append("\npublic class $javaClassName {")
            gencode.append("\n")
            entityArray.forEach {
                gencode.append("\t")
                gencode.append("private")
                gencode.append(" ")
                gencode.append(getTypeStr(it.typename))
                gencode.append(" ")
                gencode.append("${columnName2VarName(it.columnname)};")
                gencode.append("\n")
            }
            gencode.append("\n")
            entityArray.forEach {
                val varName = columnName2VarName(it.columnname)
                if (it.isPrimaryKey) {
                    gencode.append("\t@Id")
                } else {
                    gencode.append("\t@Basic")
                }
                gencode.append("\n\t@Column(name = \"${it.columnname}\")")
                gencode.append("\n\tpublic ${getTypeStr(it.typename)} get${word2JavaClassName(it.columnname)}() {")
                gencode.append("\n")
                gencode.append("\t\treturn $varName;")
                gencode.append("\n\t}")
                gencode.append("\n")
                gencode.append("\n\tpublic void set${word2JavaClassName(it.columnname)}(${getTypeStr(it.typename)} $varName) {")
                gencode.append("\n\t\tthis.$varName = $varName;")
                gencode.append("\n\t}")
                gencode.append("\n")
                gencode.append("\n")
            }

            gencode.append("\t@Override")
            gencode.append("\n\tpublic boolean equals(Object o) {")
            gencode.append("\n\t\tif (this == o) return true;")
            gencode.append("\n\t\tif (o == null || getClass() != o.getClass()) return false;")
            gencode.append("\n\t\t$javaClassName $classVarName = ($javaClassName) o;")
            gencode.append("\n\t\treturn ")
            entityArray.forEachIndexed { id, it ->
                val varName = columnName2VarName(it.columnname)
                if (id == entityArray.size - 1) {
                    gencode.append("Objects.equals($varName, $classVarName.$varName)")
                } else {
                    gencode.append("Objects.equals($varName, $classVarName.$varName) && ")
                }
            }
            gencode.append(";")
            gencode.append("\n\t}")
            gencode.append("\n")
            gencode.append("\n\t@Override")
            gencode.append("\n\tpublic int hashCode() {")
            gencode.append("\n\t\treturn Objects.hash(")
            entityArray.forEachIndexed { index, entityModel ->
                val varName = columnName2VarName(entityModel.columnname)
                if (index == entityArray.size - 1) {
                    gencode.append("$varName")
                } else {
                    gencode.append("$varName, ")
                }
            }
            gencode.append(");")
            gencode.append("\n\t}")
            gencode.append("\n}")
        }
        val folderPath = "$exportFolderPath/$connectionName/$schema/$language"
        val folder = File(folderPath)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        val javaFileName = "$javaClassName.${if (language.equals("java")) "java" else "kt"}"
        val fullPath = "$folderPath/$javaFileName"
        saveFile(fullPath, gencode.toString())
    }

    private fun getTypeStr(typename: String): String {
        val javaType: String
        if (typename.equals("VARCHAR") || typename.equals("CHAR") || typename.equals("LONGTEXT") || typename.equals("ENUM") || typename.equals(
                "SET"
            ) || typename.equals("TEXT") || typename.equals("TINYTEXT") || typename.equals("MEDIUMTEXT")
        ) {
            javaType = "String"
        } else if (typename.equals("DATETIME") || typename.equals("TIMESTAMP")) {
            javaType = "Timestamp"
        } else if (typename.equals("INT") || typename.equals("INT UNSIGNED") || typename.equals("SMALLINT UNSIGNED") || typename.equals(
                "TINYINT UNSIGNED"
            )
        ) {
            javaType = "Integer"
        } else if (typename.equals("BIGINT") || typename.equals("BIGINT UNSIGNED")) {
            javaType = "Long"
        } else if (typename.equals("FLOAT")) {
            javaType = "Float"
        } else if (typename.equals("BLOB") || typename.equals("TINYBLOB") || typename.equals("BINART") || typename.equals(
                "LONGBLOB"
            ) || typename.equals("MEDIUMBLOB")
        ) {
            javaType = "byte[]"
        } else if (typename.equals("DOUBLE")) {
            javaType = "Double"
        } else {
            javaType = "Object"
        }
        return javaType
    }

    private fun columnName2VarName(word: String): String {
        return firstLetter2LowerCase(word2JavaClassName(word))
    }

    private fun firstLetter2LowerCase(word: String): String {
        val sb = StringBuffer()
        val chars = word.toCharArray()
        chars.forEachIndexed { id, it ->
            if (id == 0) {
                sb.append(it.lowercaseChar())
            } else {
                sb.append(it)
            }
        }
        return sb.toString()
    }

    private fun word2JavaClassName(word: String): String {
        val sb = StringBuffer()
        word.split("_").forEach {
            val str = StringBuffer()
            val charArr = it.toCharArray()
            for (i in charArr.indices) {
                if (i == 0) {
                    str.append(charArr[i].uppercaseChar())
                } else {
                    str.append(charArr[i])
                }
            }
            sb.append(str)
        }
        return sb.toString()
    }
}