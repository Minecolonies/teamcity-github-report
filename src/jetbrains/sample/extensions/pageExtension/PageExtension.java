/*
 * Copyright 2000-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.sample.extensions.pageExtension;

import javax.servlet.http.HttpServletRequest;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PlaceId;
import jetbrains.buildServer.web.openapi.SimplePageExtension;
import jetbrains.buildServer.web.util.WebUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Yegor.Yarko
 *         Date: 11.03.2009
 */
public class PageExtension extends SimplePageExtension {
  public PageExtension(PagePlaces pagePlaces) {
    super(pagePlaces, PlaceId.ALL_PAGES_FOOTER, "samplePlugin", "input.jsp");
    register();
  }

  @Override
  public boolean isAvailable(@NotNull final HttpServletRequest request) {
    final String path = WebUtil.getPathWithoutAuthenticationType(request);
    return path.startsWith("/overview.jsp") || path.startsWith("/overview.html") || path.equals("/");
  }
}
