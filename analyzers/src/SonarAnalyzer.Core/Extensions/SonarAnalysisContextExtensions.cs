﻿/*
 * SonarAnalyzer for .NET
 * Copyright (C) 2014-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
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

using System.IO;

namespace SonarAnalyzer.Core.Extensions;

public static class SonarAnalysisContextExtensions
{
    private static readonly SourceTextValueProvider<ProjectConfigReader> ProjectConfigProvider = new(x => new ProjectConfigReader(x));

    public static ProjectConfigReader ProjectConfiguration(this SonarAnalysisContext context, AnalyzerOptions options)
    {
        if (options.SonarProjectConfig() is { } sonarProjectConfig)
        {
            return sonarProjectConfig.GetText() is { } sourceText
                // TryGetValue catches all exceptions from SourceTextValueProvider and returns false when exception is thrown
                && context.TryGetValue(sourceText, ProjectConfigProvider, out var cachedProjectConfigReader)
                ? cachedProjectConfigReader
                : throw new InvalidOperationException($"File '{Path.GetFileName(sonarProjectConfig.Path)}' has been added as an AdditionalFile but could not be read and parsed.");
        }
        else
        {
            return ProjectConfigReader.Empty;
        }
    }

    public static bool IsRazorAnalysisEnabled(this SonarAnalysisContext context, AnalyzerOptions options, Compilation compilation) =>
        context.ProjectConfiguration(options).ProjectType != ProjectType.Unknown
        && options.SonarLintXml(context).AnalyzeRazorCode(compilation.Language);
}
