/*
 * This file is part of the Weborganic XMLDoclet library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.xmldoclet;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.tools.doclets.Taglet;

/**
 * Container for the options for the XML Doclet.
 *
 * @author Christophe Lauret
 *
 * @version 21 June 2010
 */
public final class Options {

  /**
   * An empty array constant for reuse.
   */
  private static final String[] EMPTY_ARRAY = new String[]{};

  /**
   * The default encoding for the output
   */
  private static final Charset DEFAULT_CHARSET = Charset.forName("utf-8");

  /**
   * The default filename for the output.
   */
  private static final String DEFAULT_FILENAME = "xmldoclet.xml";

  /**
   * Determines whether the output is a single file or multiple files.
   *
   * Populated from the command line via the "-multiple" flag.
   */
  private boolean multipleFiles = false;

  /**
   * Determines whether files are organised as subfolders or all in the same folder.
   *
   * Populated from the command line via the "-subfolders" flag.
   */
  private boolean subFolders = false;

  /**
   * Determines the directory where output is placed.
   *
   * Populated from the command line via the "-d [directory]" flag.
   */
  private File directory;

  /**
   * The output encoding of the XML files.
   */
  private Charset encoding = DEFAULT_CHARSET;

  /**
   * Filter classes extending the specified class.
   */
  private String extendsFilter = null;

  /**
   * Filter classes implementing the specified class.
   */
  private String implementsFilter = null;

  /**
   * Filter classes with the specified annotation.
   */
  private String annotationFilter = null;

  /**
   * The taglets loaded by this doclet.
   */
  private Map<String, Taglet> taglets = new HashMap<String, Taglet>();

  /**
   * Name of the file - used for single output only.
   *
   * Populated from the command line via the "-filename [file]" flag.
   */
  private String filename = DEFAULT_FILENAME;

  /**
   * Creates new options.
   */
  public Options() {
    // Load the standard taglets
    for (BlockTag t : BlockTag.values()) {
      this.taglets.put(t.getName(), t);
    }
    for (InlineTag t : InlineTag.values()) {
      this.taglets.put("@"+t.getName(), t);
    }
  }

  /**
   * Indicates whether these options should use multiple files.
   */
  public boolean useMultipleFiles() {
    return this.multipleFiles;
  }

  /**
   * Indicates whether to organise files as subfolders for packages.
   */
  public boolean useSubFolders() {
    return this.subFolders;
  }

  /**
   * Returns the charset to use to encode the output.
   *
   * @return the charset to use to encode the output.
   */
  public Charset getEncoding() {
    return this.encoding;
  }

  /**
   * Returns the directory where to store the files.
   *
   * @return where to store the files.
   */
  public File getDirectory() {
    return this.directory;
  }

  /**
   * Returns the name of the file for single output.
   *
   * @return the name of the file for single output.
   */
  public String getFilename() {
    return this.filename;
  }

  /**
   * Returns the taglet instance for the specified tag name.
   *
   * @param name The name of the tag.
   * @return The corresponding <code>Taglet</code> or <code>null</code>.
   */
  public Taglet getTagletForName(String name) {
    for (String n : this.taglets.keySet()) {
      if (n.equals(name)) return this.taglets.get(n);
    }
    return null;
  }

  /**
   * Indicates whether these options specify a filter.
   *
   * @return <code>true</code> if the class must implement or extends or have a specific annotation.
   *         <code>false</code> otherwise.
   */
  public boolean hasFilter() {
    return this.extendsFilter != null || this.implementsFilter != null || this.annotationFilter != null;
  }

  /**
   * Filters the included set of classes by checking whether the given class matches all of
   * the specified '-extends', '-implements' and '-annotated' options.
   *
   * @param doc the class documentation.
   * @return <code>true</code> if the class should be included; <code>false</code> otherwise.
   */
  public boolean filter(ClassDoc doc) {
    boolean included = true;

    // Extends
    if (this.extendsFilter != null) {
      included = included && filterExtends(doc, this.extendsFilter);
    }
    // Implements
    if (this.implementsFilter != null) {
      included = included && filterImplements(doc, this.implementsFilter);
    }
    // Annotation
    if (this.annotationFilter != null) {
      included = included && filterAnnotated(doc, this.annotationFilter);
    }

    // No filtering
    return included;
  }

  @Override
  public String toString() {
    return super.toString();
  }

  // static methods for use by Doclet =============================================================

  /**
   * A JavaDoc option parsing handler.
   *
   * <p>This one returns the number of arguments required for the given option.
   *
   * @see com.sun.javadoc.Doclet#optionLength(String)
   *
   * @param option The name of the option.
   *
   * @return The number of arguments for that option.
   */
  public static int getLength(String option) {
    // possibly specified by javadoc understood by this doclet
    if ("-d".equals(option)) return 2;
    if ("-docencoding".equals(option)) return 2;

    // specific to this doclet
    if ("-multiple".equals(option)) return 1;
    if ("-filename".equals(option)) return 2;
    if ("-implements".equals(option)) return 2;
    if ("-extends".equals(option)) return 2;
    if ("-annotated".equals(option)) return 2;
    if ("-tag".equals(option)) return 2;
    if ("-taglet".equals(option)) return 2;
    if ("-subfolders".equals(option)) return 1;
    return 0;
  }

  /**
   * Retrieve the expected options from the given array of options.
   *
   * @param root The root object which contains the options to be retrieved.
   */
  public static Options toOptions(String options[][], DocErrorReporter reporter) {
    Options o = new Options();

    // Flags
    o.multipleFiles = has(options, "-multiple");
    o.subFolders    = has(options, "-subfolders");

    // Output directory
    if (has(options, "-d")) {
      String directory = get(options, "-d");
      if (directory == null) {
        reporter.printError("Missing value for <directory>, usage:");
        reporter.printError("-d <directory> Destination directory for output files");
        return null;
      } else {
        o.directory = new File(directory);
        // TODO check
        reporter.printNotice("Output directory: "+directory);
      }
    } else {
      reporter.printError("Output directory not specified; use -d <directory>");
      return null;
    }

    // Output encoding
    if (has(options, "-docencoding")) {
      String encoding = get(options, "-docencoding");
      if (encoding == null) {
        reporter.printError("Missing value for <name>, usage:");
        reporter.printError("-docencoding <name> \t Output encoding name");
        return null;
      } else {
        o.encoding = Charset.forName(encoding);
        reporter.printNotice("Output encoding: "+o.encoding);
      }
    }

    // Extends
    if (has(options, "-filename")) {
      String name = get(options, "-filename");
      if (name != null && !o.multipleFiles) {
        o.filename = name;
        reporter.printNotice("Using file name: "+name);
      } else reporter.printWarning("'-filename' option ignored");
    }

    // Extends
    if (has(options, "-extends")) {
      String superclass = get(options, "-extends");
      if (superclass != null) {
        o.extendsFilter = superclass;
        reporter.printNotice("Filtering classes extending: "+superclass);
      } else reporter.printWarning("'-extends' option ignored - superclass not specified");
    }

    // Annotated
    if (has(options, "-annotated")) {
      String annotation = get(options, "-annotated");
      if (annotation != null) {
        o.annotationFilter = annotation;
        reporter.printNotice("Filtering classes annotated: "+annotation);
      } else reporter.printWarning("'-annotated' option ignored - annotation not specified");
    }

    // Implements
    if (has(options, "-implements")) {
      String iface = get(options, "-implements");
      if (iface != null) {
        o.implementsFilter = iface;
        reporter.printNotice("Filtering classes implementing: "+iface);
      } else reporter.printWarning("'-implements' option ignored - interface not specified");
    }

    // Custom Tags
    if (has(options, "-tag")) {
      List<String> tags = getAll(options, "-tag");
      for (String def : tags) {
        int colon = def.indexOf(':');
        String name = colon < 0? def : def.substring(0, colon);
        CustomTag tag = new CustomTag(name, false);
        if (colon >= 0) {
          // scope
          String scope = def.substring(colon+1);
          colon = scope.indexOf(':');
          if (colon >= 0) {
            String title = scope.substring(colon+1);
            scope = scope.substring(0, colon);
            tag.setTitle(title);
          }
          tag.setScope(scope);
        }
        o.taglets.put(name, new CustomTag(name, true));
        reporter.printNotice("Using Tag "+name);
      }
    }

    // Taglets
    if (has(options, "-taglet")) {
      String classes = get(options, "-taglet");
      if (classes != null) {
        for (String c : classes.split(":")) {
          try {
            Class<?> x = Class.forName(c);
            Class<? extends Taglet> t = x.asSubclass(Taglet.class);
            Method m = t.getMethod("register", Map.class);
            m.invoke(null, o.taglets);
            reporter.printNotice("Using Taglet "+t.getName());
          } catch (Exception ex) {
            reporter.printError("'-taglet' option reported error - :"+ex.getMessage());
          }
        }
      } else reporter.printWarning("'-taglet' option ignored - classes not specified");
    }

    // If we reached this point everything is OK
    return o;
  }

  /**
   * Indicates whether the specified option is defined.
   *
   * @param options the matrix of command line options.
   * @param name    the name of the requested option.
   *
   * @return <code>true</code> if defined; <code>false</code> otherwise.
   */
  private static boolean has(String[][] options, String name) {
    for (String[] option : options) {
      if (option[0].equals(name)) return true;
    }
    return false;
  }

  /**
   * Returns the list of single values for the specified option if defined.
   *
   * @param options the matrix of command line options.
   * @param name    the name of the requested option.
   *
   * @return the array value if available or <code>null</code>.
   */
  private static List<String> getAll(String[][] options, String name) {
    List<String> values = new ArrayList<String>();
    for (String[] option : options) {
      if (option[0].equals(name)) {
        if (option.length > 1) values.add(option[1]);
      }
    }
    return values;
  }

  /**
   * Returns the single value for the specified option if defined.
   *
   * @param options the matrix of command line options.
   * @param name    the name of the requested option.
   *
   * @return the value if available or <code>null</code>.
   */
  private static String get(String[][] options, String name) {
    String[] option = find(options, name);
    return (option.length > 1)? option[1] : null;
  }

  /**
   * Finds the options array for the specified option name.
   *
   * <p>The first element is <i>always</i> the name of the option.
   *
   * @param options the matrix of command line options.
   * @param name    the name of the requested option.
   *
   * @return the corresponding array or an empty array.
   */
  private static String[] find(String[][] options, String name) {
    for (String[] option : options) {
      if (option[0].equals(name)) return option;
    }
    // Option not available
    return EMPTY_ARRAY;
  }

  /**
   * Filters the included set of classes by checking whether the given class matches the '-extends' option.
   *
   * @param doc  the class documentation.
   * @param base the class to extend.
   * @return <code>true</code> if the class should be included; <code>false</code> otherwise.
   */
  private static boolean filterExtends(ClassDoc doc, String base) {
    ClassDoc superclass = doc.superclass();
    return superclass != null && base.equals(superclass.toString());
  }

  /**
   * Filters the included set of classes by checking whether the given class matches the '-implements' option.
   *
   * @param doc   the class documentation.
   * @param iface the interface to implement.
   * @return <code>true</code> if the class should be included; <code>false</code> otherwise.
   */
  private static boolean filterImplements(ClassDoc doc, String iface) {
    ClassDoc[] interfaces = doc.interfaces();
    for (ClassDoc i : interfaces) {
      if (iface.equals(i.toString())) return true;
    }
    return false;
  }

  /**
   * Filters the included set of classes by checking whether the given class matches the '-annotated' option.
   *
   * @param doc        the class documentation.
   * @param annotation the annotation to match.
   * @return <code>true</code> if the class should be included; <code>false</code> otherwise.
   */
  private static boolean filterAnnotated(ClassDoc doc, String annotation) {
    AnnotationDesc[] annotations = doc.annotations();
    for (AnnotationDesc i : annotations) {
      if (annotation.equals(i.annotationType().qualifiedName())) return true;
    }
    return false;
  }
}
