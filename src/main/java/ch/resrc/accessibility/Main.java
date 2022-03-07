package ch.resrc.accessibility;

import org.asciidoctor.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.nio.file.attribute.PosixFilePermission.*;
import static org.asciidoctor.Options.builder;

public class Main {

    public static void main(String[] args) throws URISyntaxException, IOException {
        try (Asciidoctor asciidoctor = Asciidoctor.Factory.create()) {
            loadAsciiDocLibs(asciidoctor);
            cleanUpExistingDocs();
            createNewDocs(asciidoctor);
        }
    }

    private static void loadAsciiDocLibs(Asciidoctor asciidoctor) {
        asciidoctor.requireLibrary("asciidoctor-revealjs");
        asciidoctor.requireLibrary("asciidoctor-diagram");
    }

    private static void cleanUpExistingDocs() throws IOException {
        Path docsFolder = Path.of("./docs");
        Path imagesFolder = Path.of("./docs/images");
        Path stylesFolder = Path.of("./docs/styles");

        deleteIfExists(imagesFolder);
        deleteIfExists(stylesFolder);
        deleteIfExists(docsFolder);
        createFolders(docsFolder, imagesFolder, stylesFolder);
    }

    private static void createFolders(Path... folders) throws IOException {
        for (Path folder : folders) {
            create(folder);
        }
    }

    private static void createNewDocs(Asciidoctor asciidoctor) throws URISyntaxException, IOException {
        Path slides = getResource("slides.adoc");
        asciidoctor.convertFile(slides.toFile(),
                builder()
                        .backend("revealjs")
                        .safe(SafeMode.UNSAFE)
                        .attributes(Attributes.builder()
                                .attribute("revealjsdir", "https://cdn.jsdelivr.net/npm/reveal.js@3.9.2")
                                .build()
                        )
                        //.mkDirs(true)
                        .templateCache(false)
                        .toFile(new File("./docs/index.html"))
                        .build()
        );

        copyResource("images", "./docs/images");
        copyResource("styles", "./docs/styles");
    }

    private static void copyResource(String resourceDir, String targertDir) {
        Path resourcePath = getResource(resourceDir);
        Path targetPath = Path.of(targertDir);
        copyFiles(resourcePath, targetPath);
    }

    private static void copyFiles(Path source, Path target) {
        try {
            try (Stream<Path> sourceFiles = Files.walk(source)) {
                sourceFiles
                        .filter(sourceFile -> !sourceFile.toFile().isDirectory())
                        .filter(sourceFile -> !target.resolve(sourceFile.getFileName()).toFile().exists())
                        .forEach(sourceFile -> {
                                    try {
                                        Files.copy(sourceFile, target.resolve(sourceFile.getFileName()));
                                    } catch (IOException e) {
                                        throw new RuntimeException(format("Could not copy from '%s' to '%s'", source, target), e);
                                    }
                                }
                        );
            }
        } catch (IOException e) {
            throw new RuntimeException(format("Could not copy from '%s' to '%s'", source, target), e);
        }
    }

    private static Path getResource(String name) {
        URL resource = Main.class.getClassLoader().getResource(name);
        if (resource == null) {
            throw new IllegalArgumentException("Slides to convert have to be specified");
        }
        URI resourceUri;
        try {
            resourceUri = resource.toURI();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("URI for slides has to be valid");
        }
        return Path.of(resourceUri);
    }

    private static void create(Path path) throws IOException {
        Files.createDirectory(
                path,
                PosixFilePermissions.asFileAttribute(EnumSet.of(
                                OWNER_READ,
                                OWNER_WRITE,
                                OWNER_EXECUTE
                        )
                )
        );
    }

    private static void deleteIfExists(Path path) {
        if (!path.toFile().exists()) {
            return;
        }

        try (Stream<Path> docInFolder = Files.walk(path)) {
            docInFolder.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            throw new RuntimeException(format("Could not delete folder %s", path), e);
        }
    }
}
