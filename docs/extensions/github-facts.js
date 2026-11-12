'use strict'

// Reads the latest release, the stars, and the forks of the repository from the GitHub API once per build.
// The header partial (docs/supplemental-ui/partials/header-content.hbs) prints them from env.VL_GITHUB_*.
// The reader's browser doesn't call GitHub.
//
// If GitHub can't be reached or limits the request, the build goes on and the header shows only the
// repository name. With GITHUB_TOKEN set, the requests are authenticated and have a higher rate limit.

const REPO = 'vulnlog/vulnlog'
const API = 'https://api.github.com/repos/' + REPO
const TIMEOUT_MS = 5000

function compact (n) {
  return n >= 1000 ? (Math.round(n / 100) / 10).toString().replace(/\.0$/, '') + 'k' : String(n)
}

module.exports.register = function () {
  const logger = this.getLogger('github-facts')

  this.once('playbookBuilt', async ({ playbook }) => {
    const env = playbook.env
    const headers = { Accept: 'application/vnd.github+json', 'User-Agent': 'antora-docs-build' }
    if (env.GITHUB_TOKEN) headers.Authorization = 'Bearer ' + env.GITHUB_TOKEN

    const get = (url) =>
      fetch(url, { headers, signal: AbortSignal.timeout(TIMEOUT_MS) })
        .then((res) => (res.ok ? res.json() : null))
        .catch(() => null)

    const [repo, release] = await Promise.all([get(API), get(API + '/releases/latest')])
    if (!repo && !release) {
      logger.warn('GitHub API not reachable. The header shows the repository name only.')
      return
    }
    if (release && release.tag_name) env.VL_GITHUB_VERSION = release.tag_name
    if (repo && typeof repo.stargazers_count === 'number') env.VL_GITHUB_STARS = compact(repo.stargazers_count)
    if (repo && typeof repo.forks_count === 'number') env.VL_GITHUB_FORKS = compact(repo.forks_count)
  })

  // The values belong to this build only.
  this.once('sitePublished', ({ playbook }) => {
    delete playbook.env.VL_GITHUB_VERSION
    delete playbook.env.VL_GITHUB_STARS
    delete playbook.env.VL_GITHUB_FORKS
  })
}
