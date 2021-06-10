import java.nio.file.Files
import java.nio.file.Paths

/**
 * 将文本写入到文件
 */
fun saveFile(path: String, text: String) {
    val fPath = Paths.get(path)
    val bw = Files.newBufferedWriter(fPath)
    bw.write(text)
    bw.flush()
    bw.close()
}