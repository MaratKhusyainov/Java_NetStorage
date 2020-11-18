import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ListMessage extends AbstractMessage {
    private List<FileManager> filesList;

    public List<FileManager> getFilesList() {
        return filesList;
    }

    public void createList(Path path) throws IOException {
        filesList = Files.list(path)
                .map(FileManager::new)
                .collect(Collectors.toList());
    }

}
