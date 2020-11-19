import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.IOException;
import java.nio.file.*;

public class ChatHandler extends SimpleChannelInboundHandler<AbstractMessage> {
    private Path path = Path.of("cloud", "ServerStorage");
    private final String PATH = path.toString();
    private Users u = new Users();
    private String nick;

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Client connected");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("Client disconnected");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AbstractMessage msg) throws Exception {
        if (msg instanceof CommandMessage) {
            CommandMessage cm = (CommandMessage) msg;
            switch (cm.getCommand()) {
                case AUTH:
                    nick = u.getNick(cm.getParam(), cm.getSecondParam());
                    if (nick != null) {
                        ctx.writeAndFlush(new CommandMessage(Command.AUTH, nick));
                        return;
                    }
                    break;
                case SIGN_UP:
                    String s = u.addNick(cm.getParam(), cm.getSecondParam(), cm.getThirdParam());
                    ctx.writeAndFlush(new CommandMessage(Command.SIGN_UP, s));
                    break;
                case STORAGE_FILES_LIST:
                    ListMessage lm = new ListMessage();
                    if (Files.exists(Paths.get(PATH, nick))) {
                        if (cm.getParam().equals(nick)) {
                            lm.createList(Paths.get(PATH, nick));
                            ctx.writeAndFlush(lm);
                        } else {
                            lm.createList(Paths.get(cm.getParam()));
                            ctx.writeAndFlush(lm);
                        }
                    } else {
                        Files.createDirectory(Paths.get(PATH, nick));
                        Files.createFile(Paths.get(PATH, nick + "/.txt"));
                        lm.createList(Paths.get(PATH, nick));
                        ctx.writeAndFlush(lm);
                    }
                    break;
                case DIRECTORY_FILES_LIST:
                    ListMessage l = new ListMessage();
                    if (Files.exists(Paths.get(PATH, nick))) {
                        l.createList(Paths.get(PATH, nick));
                        ctx.writeAndFlush(l);
                    }
                    break;
                case FILE_REQUEST:
                    if (Files.exists(Paths.get(cm.getParam() + "/" + cm.getSecondParam()))) {
                        FileMessage fm = new FileMessage(Paths.get(cm.getParam() + "/" + cm.getSecondParam()));
                        ctx.writeAndFlush(fm);
                    }
                    break;
                case FILE_DELETE:
                    Files.deleteIfExists(Paths.get(cm.getParam()));
                    Path p = Paths.get(cm.getParam());
                    ctx.writeAndFlush(new CommandMessage(Command.STORAGE_FILES_LIST, p.getParent().toString()));
                    break;
                case FILE_RENAME:
                    changeFileName(Paths.get(cm.getThirdParam(), cm.getParam()), cm.getSecondParam());
                    ctx.writeAndFlush(new CommandMessage(Command.STORAGE_FILES_LIST, cm.getThirdParam()));
                    break;
                case FILE_DIR:
                    ListMessage m = new ListMessage();
                    m.createList(Paths.get(cm.getParam()));
                    ctx.channel().writeAndFlush(m);
                    break;
                case CREATE_DIR:
                    if (!Files.exists(Paths.get(cm.getParam() + cm.getSecondParam()))) {
                        Files.createDirectory(Paths.get(cm.getParam() + "/" + cm.getSecondParam()));
                        Files.createFile(Paths.get(cm.getParam() + "/" + cm.getSecondParam() + "/.txt"));
                        ctx.writeAndFlush(new CommandMessage(Command.STORAGE_FILES_LIST, cm.getParam()));
                    }
            }
        }
        if (msg instanceof FileMessage) {
            FileMessage fm = (FileMessage) msg;
            Files.write(Paths.get(fm.getStoragePath() + "/" + fm.getName()), fm.getData(), StandardOpenOption.CREATE_NEW);
            ctx.writeAndFlush(new CommandMessage(Command.STORAGE_FILES_LIST, fm.getStoragePath()));

        }
    }

    private void changeFileName(Path path, String newFileName) throws IOException {
        Path newName = path.resolveSibling(newFileName);
        Files.move(path, newName, StandardCopyOption.REPLACE_EXISTING);
    }
}
