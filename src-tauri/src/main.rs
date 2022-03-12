#![cfg_attr(
  all(not(debug_assertions), target_os = "windows"),
  windows_subsystem = "windows"
)]

use tauri::api::cli::get_matches;
use tauri::WindowBuilder;
use tauri::Manager;
use url::Url;

fn main() {
  let context = tauri::generate_context!();

  tauri::Builder::default()
    .setup(|app| {
      let mut window = app.get_window("main").unwrap();

      println!("is main visible? {:?}", window.is_visible());

      match get_matches(app.config().tauri.cli.as_ref().unwrap(), app.package_info()) {
        // `matches` here is a Struct with { args, subcommand }.
        // `args` is `HashMap<String, ArgData>` where `ArgData` is a struct with { value, occurances }.
        // `subcommand` is `Option<Box<SubcommandMatches>>` where `SubcommandMatches` is a struct with { name, matches }.
        Ok(matches) => {
            match matches.subcommand {
              Some(sub) => {
                match sub.name.as_str() {
                  "create-window" => {
                    let title = &sub.matches.args.get("title").clone().unwrap().value;
                    let label = &sub.matches.args.get("label").clone().unwrap().value;
                    let url = &sub.matches.args.get("url").clone().unwrap().value;
                    println!("title {:?}", title);
                    println!("url {:?}", url.as_str());
                    println!("label {:?}", label);

                    let proper_url = Url::parse(&url.as_str().unwrap()).unwrap();

                    window.create_window(
                        label.as_str().unwrap().to_string(),
                        tauri::WindowUrl::External(proper_url),
                        |window_builder, webview_attributes| {
                          println!("\n{:?}", webview_attributes);
                          (window_builder
                           .title(title.as_str().unwrap())
                           .resizable(true)
                           .visible(true)
                           .transparent(true)
                           .position(0.0, 0.0)
                           .inner_size(800.0, 800.0)
                           .focus()
                           ,
                           webview_attributes)
                        },
                      ).unwrap();

                    // new_window.show();

                  }
                  _ => {

                    println!("no matching sub.name");
                  }
                }
              }
              _ => {
                println!("no matches.subcommand");
              }
            }
        }
        Err(_) => {

                println!("total miss-match!");
        }
      };

      Ok(())
    })
    .run(context)
    .expect("error while running tauri application");

  println!("end of main fn");
}
