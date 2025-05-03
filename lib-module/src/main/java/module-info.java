module javaDistributedSnapshot.module {
    requires org.jetbrains.annotations;
    requires org.yaml.snakeyaml;
    requires java.logging;

    exports polimi.ds.dsnapshot.Api;
    exports polimi.ds.dsnapshot.Exception.ExportedException;
}