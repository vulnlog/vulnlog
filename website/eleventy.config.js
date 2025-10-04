import syntaxHighlight from "@11ty/eleventy-plugin-syntaxhighlight";
import mermaid from "@kevingimbel/eleventy-plugin-mermaid";
import markdownIt from "markdown-it";
import markdownItAnchor from "markdown-it-anchor";
import slugify from "slugify";

const linkAfterHeader = markdownItAnchor.permalink.linkAfterHeader({
  class: "anchor",
  symbol: "<span hidden>#</span>",
  style: "aria-labelledby",
});
const markdownItAnchorOptions = {
  level: [1, 2, 3],
  slugify: (str) =>
      slugify(str, {
        lower: true,
        strict: true,
        remove: /["]/g,
      }),
  tabIndex: false,
  permalink(slug, opts, state, idx) {
    state.tokens.splice(
        idx,
        0,
        Object.assign(new state.Token("div_open", "div", 1), {
          // Add class "header-wrapper [h1 or h2 or h3]"
          attrs: [["class", `heading-wrapper ${state.tokens[idx].tag}`]],
          block: true,
        })
    );

    state.tokens.splice(
        idx + 4,
        0,
        Object.assign(new state.Token("div_close", "div", -1), {
          block: true,
        })
    );

    linkAfterHeader(slug, opts, state, idx + 1);
  },
};

/* Markdown Overrides */
let markdownLibrary = markdownIt({
  html: true,
}).use(markdownItAnchor, markdownItAnchorOptions);

export default async function (eleventyConfig) {
  eleventyConfig.addPlugin(syntaxHighlight);
  eleventyConfig.addPlugin(mermaid);
  eleventyConfig.setLibrary("md", markdownLibrary);

  eleventyConfig.addPassthroughCopy({
    "./node_modules/bulma/css/bulma.min.css": "/css/bulma.min.css",
    "./src/css/*.css": "/css/",
    "./node_modules/prismjs/themes/prism-tomorrow.min.css": "/css/prism-tomorrow.min.css",
    "./src/security.txt": ".well-known/security.txt",
    "./src/favicon.ico": "/favicon.ico",
    "./src/media/*": "/media/",
  });
  eleventyConfig.addWatchTarget("./src/css/");
  eleventyConfig.addWatchTarget("./src/*.txt");

  eleventyConfig.addGlobalData("metadata", {
    url: process.env.SITE_URL || "http://localhost:8080"
  });

  return {
    dir: {
      input: "src",
      output: "public"
    }
  }
};
