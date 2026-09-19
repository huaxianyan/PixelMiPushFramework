package top.trumeet.mipushframework.main


import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xiaomi.xmsf.R
import top.trumeet.mipushframework.component.MarkdownView
import top.trumeet.mipushframework.component.NavigationRow
import top.trumeet.mipushframework.component.PageColumn
import top.trumeet.mipushframework.component.SettingsGroup
import top.trumeet.ui.theme.Theme
import java.io.InputStreamReader

/**
 * The help page is drawn entirely in Compose, so it is a plain [ComponentActivity]: an
 * AppCompat activity would put the system action bar on top of the Compose top bar and the
 * activity label would show up twice.
 */
class HelpPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Theme {
                HelpHost(onBack = { finish() })
            }
        }
    }
}

@Preview(
    showSystemUi = true,
    showBackground = true,
)
@Composable
fun HelpHostPreview() {
    HelpHost()
}

/** The help page and the article it opens, sharing one back stack. */
@Composable
fun HelpHost(modifier: Modifier = Modifier, onBack: () -> Unit = {}) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "list", modifier = modifier) {
        composable("list") { HelpList(navController, onBack) }
        composable("markdown/{markdownResId}") { backStackEntry ->
            val markdownResId = backStackEntry.arguments?.getString("markdownResId")?.toInt()
            MarkdownPage(markdownResId, onBack = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HelpList(navController: NavHostController, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.helplib_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        PageColumn(contentPadding = padding) {
            FAQ(navController)
            ContactUs()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarkdownPage(markdownResId: Int?, onBack: () -> Unit) {
    val context = LocalContext.current
    val title = getArticles(context).firstOrNull { it.markdownRes == markdownResId }
        ?.let { stringResource(it.titleRes) }
        ?: stringResource(R.string.helplib_title)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        MarkdownView(
            readRawFile(context, markdownResId!!),
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
private fun FAQ(
    navController: NavHostController
) {
    SettingsGroup(stringResource(R.string.helplib_title_faq)) {
        for (article in getArticles(LocalContext.current)) {
            NavigationRow(stringResource(article.titleRes)) {
                navController.navigate("markdown/${article.markdownRes}") // 跳转并传递数据
            }
        }
    }
}

@Composable
private fun ContactUs() {
    val context = LocalContext.current
    SettingsGroup(stringResource(R.string.helplib_title_contact)) {
        NavigationRow(stringResource(R.string.helplib_action_issue)) {
            openUrl(context, "https://github.com/huaxianyan/PixelMiPushFramework/issues")
        }
    }
}

private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

class Article(val titleRes: Int, val markdownRes: Int)

private fun getArticles(context: Context): List<Article> {
    val articlesArray = context.resources.getStringArray(R.array.help_articles)

    val articles = mutableListOf<Article>()
    for (str in articlesArray) {
        val info = str.split("|")
        if (info.size != 2) continue

        val titleRes = R.string::class.java.getField(info[0]).getInt(null)
        val markdownRes = R.raw::class.java.getField(info[1]).getInt(null)
        articles.add(Article(titleRes, markdownRes))
    }
    return articles
}

private fun readRawFile(context: Context, fileName: Int): String {
    val inputStream = context.resources.openRawResource(fileName)
    val reader = InputStreamReader(inputStream)
    return reader.readText()
}
