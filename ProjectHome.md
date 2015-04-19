A doclet for the javadoc which allows java documentation to be published as XML.

Note: There are other doctlet out there doing the same job, but most of them seem no longer maintained or do not support the new notations introduced with Java 5.

How to use:
```
javadoc -doclet org.weborganic.xmldoclet.XMLDoclet \
    -docletpath lib/wo-xmldoclet-0.8.11.jar:lib/jtidy-r938.jar \
    -sourcepath <pathlist> [packagenames] -d <directory>
```