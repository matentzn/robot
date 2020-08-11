package org.obolibrary.robot.checks;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.obolibrary.robot.IOHelper;
import org.obolibrary.robot.QuotedEntityChecker;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Report {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(Report.class);

  /** Reporting level INFO. */
  private static final String INFO = "INFO";

  /** Reporting level WARN. */
  private static final String WARN = "WARN";

  /** Reporting level ERROR. */
  private static final String ERROR = "ERROR";

  /** New line separator. */
  private static final String NEW_LINE = System.getProperty("line.separator");

  /** Map of rules and the violations for INFO level. */
  public Map<String, List<Violation>> info = new HashMap<>();

  /** Map of rules and the violations for WARN level. */
  public Map<String, List<Violation>> warn = new HashMap<>();

  /** Map of rules and the violations for ERROR level. */
  public Map<String, List<Violation>> error = new HashMap<>();

  /** Boolean to use labels for output - defaults to false. */
  private boolean useLabels = false;

  /** Count of violations for INFO. */
  private Integer infoCount = 0;

  /** Count of violations for WARN. */
  private Integer warnCount = 0;

  /** Count of violations for ERROR. */
  private Integer errorCount = 0;

  /** IOHelper to use. */
  private IOHelper ioHelper;

  /** QuotedEntityChecker to use. */
  private QuotedEntityChecker checker = null;

  /**
   * Create a new report object without an ontology or predefined IOHelper.
   *
   * @throws IOException on problem creating IOHelper
   */
  public Report() throws IOException {
    ioHelper = new IOHelper();
  }

  /**
   * Create a new report object with an ontology (maybe) using labels for output.
   *
   * @param ontology OWLOntology to get labels from
   * @param useLabels if true, use labels for output
   * @throws IOException on problem creating IOHelper
   */
  public Report(OWLOntology ontology, boolean useLabels) throws IOException {
    ioHelper = new IOHelper();

    if (useLabels) {
      checker = new QuotedEntityChecker();
      checker.setIOHelper(this.ioHelper);
      checker.addProvider(new SimpleShortFormProvider());
      checker.addProperty(OWLManager.getOWLDataFactory().getRDFSLabel());
      if (ontology != null) {
        checker.addAll(ontology);
      }
    }
  }

  /**
   * Create a new report object with a QuotedEntityChecker loaded with entries from the label map.
   * Use labels for report output.
   *
   * @param labelMap Map of IRI to label for all entities in the ontology
   * @throws IOException on problem creating IOHelper
   */
  public Report(Map<IRI, String> labelMap) throws IOException {
    this.ioHelper = new IOHelper();
    checker = new QuotedEntityChecker();
    checker.setIOHelper(ioHelper);
    checker.addProvider(new SimpleShortFormProvider());
    checker.addProperty(OWLManager.getOWLDataFactory().getRDFSLabel());
    if (labelMap != null) {
      useLabels = true;
      OWLDataFactory df = OWLManager.getOWLDataFactory();
      for (Entry<IRI, String> entry : labelMap.entrySet()) {
        // Set all the entities as class - will not matter for retrieving label
        OWLEntity e = df.getOWLEntity(EntityType.CLASS, entry.getKey());
        checker.add(e, entry.getValue());
      }
    }
  }

  /**
   * Create a new report object with an ontology to (maybe) get labels from and a defined IOHelper.
   *
   * @param ontology OWLOntology to get labels from
   * @param ioHelper IOHelper to use
   * @param useLabels if true, use labels for output
   */
  public Report(OWLOntology ontology, IOHelper ioHelper, boolean useLabels) {
    this.ioHelper = ioHelper;
    if (useLabels) {
      checker = new QuotedEntityChecker();
      checker.setIOHelper(this.ioHelper);
      checker.addProvider(new SimpleShortFormProvider());
      checker.addProperty(OWLManager.getOWLDataFactory().getRDFSLabel());
      if (ontology != null) {
        checker.addAll(ontology);
      }
    }
    this.useLabels = useLabels;
  }

  /**
   * Create a new report object with an ontology and a new IOHelper.
   *
   * @deprecated Report will not do anything with the ontology when not using labels. Use either
   *     {@link #Report()} or {@link #Report(OWLOntology, boolean)} or {@link #Report(OWLOntology,
   *     IOHelper, boolean)}.
   * @param ontology OWLOntology to get labels from
   * @throws IOException on problem creating IOHelper
   */
  @Deprecated
  public Report(OWLOntology ontology) throws IOException {
    // Ontology input doesn't do anything without labels
    ioHelper = new IOHelper();
  }

  /**
   * Create a new report object with an ontology and an IOHelper.
   *
   * @deprecated Report will not do anything with the ontology when not using labels. Use either
   *     {@link #Report(OWLOntology, boolean)} or {@link #Report(OWLOntology, IOHelper, boolean)}.
   * @param ontology OWLOntology object
   * @param ioHelper IOHelper to use
   */
  @Deprecated
  public Report(OWLOntology ontology, IOHelper ioHelper) {
    // Ontology input doesn't do anything without labels
    this.ioHelper = ioHelper;
  }

  /**
   * Given a rule name, it's reporting level, and a list of the violations from the ontology, add
   * the violations to the correct map.
   *
   * @param ruleName name of rule
   * @param level reporting level of rule
   * @param violations list of violations from this rule
   */
  public void addViolations(String ruleName, String level, List<Violation> violations) {
    logger.debug("violation found: " + ruleName);
    if (INFO.equals(level)) {
      info.put(ruleName, violations);
      infoCount += violations.size();
    } else if (WARN.equals(level)) {
      warn.put(ruleName, violations);
      warnCount += violations.size();
    } else if (ERROR.equals(level)) {
      error.put(ruleName, violations);
      errorCount += violations.size();
    }
    // Otherwise do nothing
  }

  /**
   * Return all the IRI strings in the current Violations.
   *
   * @return a set of IRI strings
   */
  public Set<String> getIRIs() {
    Set<String> iris = new HashSet<>();
    iris.addAll(getIRIs(error));
    iris.addAll(getIRIs(warn));
    iris.addAll(getIRIs(info));
    return iris;
  }

  /**
   * Return all the IRI strings in the given list of Violations.
   *
   * @param violationSets map of rule name and violations
   * @return a set of IRI strings
   */
  public Set<String> getIRIs(Map<String, List<Violation>> violationSets) {
    Set<String> iris = new HashSet<>();

    for (Entry<String, List<Violation>> vs : violationSets.entrySet()) {
      for (Violation v : vs.getValue()) {
        iris.add(v.subject);
        for (Entry<String, List<String>> statement : v.statements.entrySet()) {
          iris.add(statement.getKey());
          for (String value : statement.getValue()) {
            if (!iris.contains(value)) {
              try {
                iris.add(IRI.create(new URL(value)).toString());
              } catch (Exception e) {
                // do nothing
              }
            }
          }
        }
      }
    }

    return iris;
  }

  /**
   * Return the total violations from reporting.
   *
   * @return number of violations
   */
  public Integer getTotalViolations() {
    return infoCount + warnCount + errorCount;
  }

  /**
   * Given a reporting level, return the number of violations.
   *
   * @param level reporting level
   * @return number of violations
   */
  public Integer getTotalViolations(String level) {
    if (INFO.equals(level)) {
      return infoCount;
    } else if (WARN.equals(level)) {
      return warnCount;
    } else if (ERROR.equals(level)) {
      return errorCount;
    } else {
      logger.error("Not a valid report level: " + level);
      return 0;
    }
  }

  /**
   * Given a rule name, return the violation count for that rule. Throw exception if rule does not
   * exists.
   *
   * @param ruleName rule name to get number of violations for
   * @return number of violations for given rule name
   * @throws Exception if the rule name is not in this Report object
   */
  public Integer getViolationCount(String ruleName) throws Exception {
    if (info.containsKey(ruleName)) {
      List<Violation> v = info.get(ruleName);
      return v.size();
    } else if (warn.containsKey(ruleName)) {
      List<Violation> v = warn.get(ruleName);
      return v.size();
    } else if (error.containsKey(ruleName)) {
      List<Violation> v = error.get(ruleName);
      return v.size();
    }
    throw new Exception(String.format("'%s' is not a rule in this Report", ruleName));
  }

  /**
   * Return the report in CSV format.
   *
   * @return CSV string
   */
  public String toCSV() {
    return "Level,Rule Name,Subject,Property,Value"
        + NEW_LINE
        + csvHelper(ERROR, error)
        + csvHelper(WARN, warn)
        + csvHelper(INFO, info);
  }

  /**
   * Return the report in TSV format.
   *
   * @return TSV string
   */
  public String toTSV() {
    return "Level\tRule Name\tSubject\tProperty\tValue"
        + NEW_LINE
        + tsvHelper(ERROR, error)
        + tsvHelper(WARN, warn)
        + tsvHelper(INFO, info);
  }

  /**
   * Return the report in YAML format.
   *
   * @return YAML string
   */
  public String toYAML() {
    return yamlHelper(ERROR, error) + yamlHelper(WARN, warn) + yamlHelper(INFO, info);
  }

  /**
   * Given a reporting level and a map of rules and violations, build a CSV output.
   *
   * @param level reporting level
   * @param violationSets map of rules and violations
   * @return CSV string representation of the violations
   */
  private String csvHelper(String level, Map<String, List<Violation>> violationSets) {
    return tableHelper(level, violationSets, ",", "\"", "'");
  }

  /**
   * Given a PrefixManager and a value as a string, return the label or CURIE of the value if
   * possible. Otherwise, return the value.
   *
   * @param pm PrefixManager to use to create CURIEs
   * @param value value of cell - may be IRI or literal value
   * @return display name as label, CURIE, or the original value
   */
  private String getDisplayName(PrefixManager pm, String value) {
    String label = null;
    if (useLabels) {
      label = maybeGetLabel(value);
    }
    String curie = maybeGetCURIE(pm, value);
    if (label != null && curie != null) {
      return label + " [" + curie + "]";
    } else if (label != null) {
      return label + " <" + value + ">";
    } else if (curie != null) {
      return curie;
    } else {
      return value;
    }
  }

  /**
   * Given a rule name, return a rule name. If the string starts with "file", the name is the path
   * and should be stripped to just the name. Otherwise, the input is returned.
   *
   * @param ruleName string to (maybe) format
   * @return rule name
   */
  private String getRuleName(String ruleName) {
    if (ruleName.contains("file:")) {
      try {
        return ruleName.substring(ruleName.lastIndexOf("/") + 1, ruleName.lastIndexOf("."));
      } catch (Exception e) {
        return ruleName;
      }
    }
    return ruleName;
  }

  /**
   * Given a prefix manager and an IRI as a string, return the CURIE if the prefix is available.
   * Otherwise, return null.
   *
   * @param pm PrefixManager to use
   * @param iriString IRI to convert to CURIE
   * @return CURIE, or null
   */
  private String maybeGetCURIE(PrefixManager pm, String iriString) {
    IRI iri = IRI.create(iriString);
    String prefix = pm.getPrefixIRI(iri);
    // Strip the default OBO prefix to have ontology-specific prefixes
    if (prefix != null && prefix.startsWith("obo:")) {
      return prefix.substring(4).replace("_", ":");
    }
    return prefix;
  }

  /**
   * Given an IRI as a string, return the label if it exists in the QuotedEntityChecker. Otherwise,
   * return null.
   *
   * @param iriString IRI to find label
   * @return label of IRI, or null
   */
  private String maybeGetLabel(String iriString) {
    IRI iri = IRI.create(iriString);
    if (checker != null) {
      String label = checker.getLabel(iri);
      if (label == null || label.equals("")) {
        if (iriString.matches("[a-z0-9]{32}")) {
          // Label blank nodes
          return "blank node";
        } else {
          return null;
        }
      }
      return label;
    }
    return null;
  }

  /**
   * Given a reporting level and a map of rules and violations, build a table output.
   *
   * @param level reporting level
   * @param violationSets map of rules and violations
   * @param separator cell separator (e.g. comma)
   * @param qualifier text qualifier (e.g. double quote)
   * @param replacement text replacement for qualifier in literal values
   * @return table string representation of the violations
   */
  private String tableHelper(
      String level,
      Map<String, List<Violation>> violationSets,
      String separator,
      String qualifier,
      String replacement) {
    // Get a prefix manager for creating CURIEs
    PrefixManager pm = ioHelper.getPrefixManager();
    if (violationSets.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    // Map of rule names and their violations
    for (Entry<String, List<Violation>> vs : violationSets.entrySet()) {
      String ruleName = getRuleName(vs.getKey());
      for (Violation v : vs.getValue()) {
        String subject = getDisplayName(pm, v.subject);
        for (Entry<String, List<String>> statement : v.statements.entrySet()) {
          String property = getDisplayName(pm, statement.getKey());
          for (String value : statement.getValue()) {
            if (value == null) {
              value = "";
            } else {
              String display = getDisplayName(pm, value);
              if (!display.equals("")) {
                value = display;
              }
            }
            // Replace qualifiers, newlines, and tabs in literals
            value =
                value
                    .replace(qualifier, replacement)
                    .replace(NEW_LINE, "    ")
                    .replace("\t", "    ");
            // Build table row
            sb.append(qualifier).append(level).append(qualifier).append(separator);
            sb.append(qualifier).append(ruleName).append(qualifier).append(separator);
            sb.append(qualifier).append(subject).append(qualifier).append(separator);
            sb.append(qualifier).append(property).append(qualifier).append(separator);
            sb.append(qualifier).append(value).append(qualifier).append(separator);
            sb.append(NEW_LINE);
          }
        }
      }
    }
    return sb.toString();
  }

  /**
   * Given a reporting level and a map of rules and violations, build a TSV output.
   *
   * @param level reporting level
   * @param violationSets map of rules and violations
   * @return TSV string representation of the violations
   */
  private String tsvHelper(String level, Map<String, List<Violation>> violationSets) {
    return tableHelper(level, violationSets, "\t", "", "");
  }

  /**
   * Given a reporting level and a map of rules and violations, build a YAML output.
   *
   * @param level reporting level
   * @param violationSets map of rules and violations
   * @return YAML string representation of the violations
   */
  private String yamlHelper(String level, Map<String, List<Violation>> violationSets) {
    // Get a prefix manager for creating CURIEs
    PrefixManager pm = ioHelper.getPrefixManager();
    if (violationSets.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("- level : '").append(level).append("'");
    sb.append(NEW_LINE);
    sb.append("  violations :");
    sb.append(NEW_LINE);
    for (Entry<String, List<Violation>> vs : violationSets.entrySet()) {
      String ruleName = getRuleName(vs.getKey());
      if (vs.getValue().isEmpty()) {
        continue;
      }
      sb.append("  - rule : '").append(ruleName).append("'");
      sb.append(NEW_LINE);
      for (Violation v : vs.getValue()) {
        String subject = getDisplayName(pm, v.subject);
        sb.append("    - subject : '").append(subject).append("'");
        sb.append(NEW_LINE);
        for (Entry<String, List<String>> statement : v.statements.entrySet()) {
          String property = getDisplayName(pm, statement.getKey());
          sb.append("      property : '").append(property).append("':");
          sb.append(NEW_LINE);
          if (statement.getValue().isEmpty()) {
            continue;
          }
          sb.append("      values :");
          sb.append(NEW_LINE);
          for (String value : statement.getValue()) {
            if (value == null) {
              value = "";
            } else {
              String display = getDisplayName(pm, value);
              if (!display.equals("")) {
                value = display;
              }
            }
            sb.append("        - '").append(value).append("'");
            sb.append(NEW_LINE);
          }
        }
      }
    }
    return sb.toString();
  }
}
