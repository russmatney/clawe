#![cfg_attr(
  all(not(debug_assertions), target_os = "windows"),
  windows_subsystem = "windows"
)]

use tauri::{Menu, Submenu, CustomMenuItem};
// use tauri::{Menu, WindowMenuEvent, Wry, Submenu, CustomMenuItem, WindowUrl, WindowBuilder};
// use url::Url;


/// Creates the main menu of the application
pub fn create() -> Menu {
    let test_menu = Menu::new()
        .add_item(CustomMenuItem::new("new_window", "Create new window"));

    Menu::new()
        .add_submenu(Submenu::new("Menu", test_menu))

}

/// Handles the various events that come from the application's menu
/* pub fn handler(event: WindowMenuEvent<Wry>) {
    match event.menu_item_id() {
        "new_window" => {
            let mut window = event.window().clone();
            window
                .create_window(
                    "acknowledgements".to_string(),
                  WindowUrl::App(),
                    move |window_builder, webview_attributes| {
                        (
                            window_builder.resizable(false).center().visible(true),
                            webview_attributes,
                        )
                    },
                )
                .unwrap();
        }
        _ => {}
    }
} */


#[tauri::command]
fn my_custom_command() {
  println!("I am an invoked rust command!");
}

fn main() {
  tauri::Builder::default()
      // .menu(create())
      // .on_menu_event(handler)
      .invoke_handler(tauri::generate_handler![my_custom_command])
    .run(tauri::generate_context!())
    .expect("error while running tauri application");
}
