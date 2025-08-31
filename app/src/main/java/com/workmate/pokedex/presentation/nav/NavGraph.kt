package com.workmate.pokedex.presentation.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.workmate.pokedex.presentation.ui.FiltersScreen
import com.workmate.pokedex.presentation.ui.PokemonDetailScreen
import com.workmate.pokedex.presentation.ui.PokemonListScreen

/**
 * Объект с route-константами.
 * Держим строки в одном месте, чтобы не ошибаться в путях при навигации.
 *
 * LIST    – стартовый экран со списком покемонов
 * FILTERS – экран выбора фильтров
 * DETAILS – шаблон маршрута детального экрана с path-параметром {id}
 */
object Routes {
    const val LIST = "list"
    const val FILTERS = "filters"
    const val DETAILS = "details/{id}" // {id} — это плейсхолдер аргумента
}

/**
 * Корневая функция, создающая NavController и NavHost.
 *
 * - rememberNavController() — создаёт и запоминает контроллер навигации в композиции.
 * - NavHost(...) — контейнер, в котором описывается граф (набор destination’ов).
 *   Параметр startDestination задаёт начальный экран.
 *
 * Каждый destination добавляется вызовом composable(route = "...") { ... }.
 * Внутри лямбды мы вызываем соответствующий экран и прокидываем колбэки навигации.
 */
@Composable
fun AppNavHost() {
    // Создаём NavController и помещаем его в Composition, чтобы он переживал рекомпозиции.
    val nav = rememberNavController()

    // Описываем граф навигации. Стартуем со списка.
    NavHost(navController = nav, startDestination = Routes.LIST) {

        // Экран списка покемонов.
        // Кнопка «фильтры» ведёт на экран FILTERS.
        // Тап по карточке покемона — на DETAILS с конкретным id.
        composable(Routes.LIST) {
            PokemonListScreen(
                onOpenFilters = { nav.navigate(Routes.FILTERS) },
                onOpenDetails = { id -> nav.navigate("details/$id") } // подставляем id в путь
            )
        }

        // Экран фильтров.
        // Стрелка «назад» вызывает popBackStack(), чтобы вернуться на предыдущий экран.
        composable(Routes.FILTERS) {
            FiltersScreen(onBack = { nav.popBackStack() })
        }

        // Экран детали.
        // Здесь маршрут содержит аргумент {id}, поэтому описываем список аргументов
        // и их тип (NavType.IntType). Navigation сам распарсит "details/25" в Int 25.
        composable(
            route = Routes.DETAILS,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            // Достаём аргумент из back stack entry. Если его нет — выходим из composable.
            val id = backStackEntry.arguments?.getInt("id") ?: return@composable

            // Рисуем экран детали и прокидываем обработчик «назад».
            PokemonDetailScreen(id = id, onBack = { nav.popBackStack() })
        }
    }
}
