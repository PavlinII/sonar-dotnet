/*
 * SonarSource :: .NET :: Core
 * Copyright (C) 2014-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.dotnet.tests;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.scanner.ScannerSide;

@ScannerSide
public class XUnitTestResultsParser implements UnitTestResultParser {
  private static final Logger LOG = LoggerFactory.getLogger(XUnitTestResultsParser.class);

  @Override
  public void parse(File file, Map<String, UnitTestResults> unitTestResults, Map<String, String> methodFileMap) {
    LOG.info("Parsing the XUnit Test Results file '{}'.", file.getAbsolutePath());

    var parser = new Parser(file, unitTestResults, methodFileMap, List.of("assembly", "assemblies"));

    Map<String, Consumer<XmlParserHelper>> tagHandlers = Map.of(
      "assembly", parser::handleAssemblyTag,
      "test", parser::handleTestTag
    );

    parser.parse(tagHandlers);
  }

  private static class Parser extends XmlTestReportParser {
    private String dllName;

    Parser(File file,  Map<String, UnitTestResults> unitTestResults, Map<String, String> methodFileMap, List<String> rootTags) {
      super(file, unitTestResults, methodFileMap, rootTags);
    }

    private void handleAssemblyTag(XmlParserHelper xmlParserHelper) {
      var assemblyName = xmlParserHelper.getRequiredAttribute("name");
      dllName = extractDllNameFromFilePath(assemblyName);
      LOG.debug("XUnit Assembly found, assembly name: {}, Extracted dllName: {}", assemblyName, dllName);
    }

    private void handleTestTag(XmlParserHelper xmlParserHelper) {
      var result = xmlParserHelper.getRequiredAttribute("result");
      var time = xmlParserHelper.getDoubleAttribute("time");
      var name = xmlParserHelper.getRequiredAttribute("name");
      var executionTime = time == null ? null : (long) (time * 1000);
      var testResults = new XUnitTestResults(result, executionTime);
      var fullName = getFullName(name, dllName);

      addTestResultToFile(fullName, testResults);
    }
  }
}
