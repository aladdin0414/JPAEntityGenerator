import java.nio.file.Files
import java.nio.file.Paths

fun main(){
    val fpath = Paths.get("/Users/liyc/test.json")
    val bw = Files.newBufferedWriter(fpath)
    bw.write("hello world!!!")
    bw.flush()
    bw.close()
}