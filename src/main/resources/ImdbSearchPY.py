# cd C:\Users\rivoi\Documents\NetBeansProjects\PycharmProjects\ImdbSearchPY
# pyinstaller --onefile main.py --icon=C:\Users\rivoi\Documents\NetBeansProjects\PycharmProjects\ImdbSearchPY\IMDb.ico --nowindowed --noconsole --paths C:\Users\rivoi\Documents\NetBeansProjects\PycharmProjects\ImdbSearchPYvenv\Lib\site-packages
# pip install git+https://github.com/cinemagoer/cinemagoer

import json
import os
import sys
import traceback
import urllib.request
from concurrent.futures.thread import ThreadPoolExecutor
from datetime import datetime

import imdb
import jsonpickle
from imdb import Cinemagoer, Company, IMDbError, Movie, Person

SUPPORTED_EXTENSIONS = None
IGNORED_FOLDERS = None
search_path = None
props: dict[str, any] = {}


def load_data(title: str) -> dict[str, None | list | tuple | dict | list]:
    prop: dict[str, None | list | tuple | dict | list] = {}
    movie = None

    try:
        isError: bool = True
        count = 0
        while isError:
            try:
                print(f"\t\t\tLooking for '{title}'")
                ia: Cinemagoer = Cinemagoer()
                movies: list[Movie] = ia.search_movie(title)
                movie = movies[0]
                movie = ia.get_movie(movie.movieID, info=["main", "plot", "awards"])
                isError = False
            except IMDbError as ex:
                count = count + 1
                isError = True
        if count == 4:
            raise ex
        print(f"\t\t\tRetrying {count} for '{title}'")

    except IMDbError as ex:
        print(f"2 ERROR {ex}: {title}")
        print(traceback.format_exc())
        raise ex
    finally:
        if movie is None:
            print(
                f"\t1 ERROR - ************** IMDbError ************** Not found: {title}"
            )
        else:
            for key in movie.infoset2keys:
                values = movie.infoset2keys[key]
                for value in values:
                    if (
                        type(movie.get(value)) is list
                        and len(movie.get(value)) > 0
                        and (
                            isinstance(movie.get(value)[0], Person.Person)
                            or isinstance(movie.get(value)[0], Company.Company)
                        )
                    ):
                        pass
                    else:
                        prop[f"{key}.{value}"] = movie.get(value)

            try:
                prop["main.casts"] = []
                for val in movie["cast"]:
                    if len(val) > 0:
                        prop["main.casts"].append(val.get("name", ""))
            except KeyError:
                pass

            try:
                prop["main.directors"] = []
                for val in movie["director"]:
                    if len(val) > 0:
                        prop["main.directors"].append(val["name"])
            except KeyError:
                pass

            try:
                prop["main.writers"] = []
                for val in movie["writer"]:
                    if len(val) > 0:
                        prop["main.writers"].append(val["name"])
            except KeyError:
                pass
    return prop


def save_json(prop: dict[str, any]) -> None:
    print(f"Writing file {OUTPUT_JSON_FILE}")
    print(prop.keys())
    with open(OUTPUT_JSON_FILE, "w", encoding="utf-8") as outfile:
        json_str: str = json.dumps(prop, indent=4, sort_keys=True)
        if len(json_str) == 0:
            print("json_str is empty, trying with jsonpickle.")
            json_str = jsonpickle.encode(prop, indent=4)
        outfile.write(json_str)


def spawn(thread_index: int, titles: list[str]):
    try:
        print(f"\tStarting thread_id: {thread_index}, Titles: {titles}")
        size: int = len(titles)
        i: int = 0
        global props
        print(titles)
        title: str
        for title in titles:
            # time.sleep(1)
            i += 1
            prop = {}
            try:
                title = title.replace(".", " ")
            except Exception:
                pass  # No dot and extension
            finally:
                try:
                    prop = load_data(title)
                except Exception:
                    try:
                        prop = load_data(title)
                    except Exception as ex:
                        print(
                            f"\t2 ERROR - ************** IMDbError ************** thread_id: {thread_index}, {ex}: {title}"
                        )
                        print(traceback.format_exc())
                finally:
                    print(f"\t\tthread_id: {thread_index}, {i}/{size}, found: {title}")
                    props.update({title: prop})
        print(f"\tEnding {thread_index}\r")
    except Exception as ex:
        print(
            f"\t3 ERROR - ************** IMDbError ************** thread_id: {thread_index}, {ex}: {titles}"
        )
        print(traceback.format_exc())


def args_search(files: list[str]):
    print(f"Searching args {files}")
    file_count: int = len(files)
    thread_nb: int = THREAD_NB
    files_per_thread: int = int(len(files) / thread_nb)
    remain_files: int = file_count - files_per_thread * thread_nb
    print(
        f"file_count: {file_count}, thread_nb: {thread_nb}, files_per_thread: {files_per_thread}, remain_files: {remain_files}, toto: {file_count / thread_nb}"
    )

    with ThreadPoolExecutor(max_workers=thread_nb + 1) as executor:
        i: int = 0
        thread_id: int
        for thread_id in range(1, thread_nb + 1):
            k: int = thread_id * files_per_thread
            print(f"thread_id: {thread_id}, {range(i, k)}, size: {len(files[i:k])}")
            executor.submit(spawn, thread_id, files[i:k])
            i = k
        print(
            f"thread_id: {thread_nb + 1}, {range(file_count - remain_files, file_count)}, size: {len(files[file_count - remain_files:file_count])}"
        )
        executor.submit(
            spawn, thread_nb + 1, files[file_count - remain_files : file_count]
        )
    print("All tasks has been finished")
    save_json(props)
    print(
        f"Threads: {thread_nb}, Time elapsed: {datetime.fromtimestamp(datetime.timestamp(datetime.now()) - start).strftime('%M:%S.%f')} for {len(files)} titles, {datetime.fromtimestamp((datetime.timestamp(datetime.now()) - start) / len(files)).strftime('%M:%S.%f')} per title."
    )


def path_search(path):
    print(f"Searching into {path}")
    if os.path.isfile(OUTPUT_JSON_FILE):
        os.remove(OUTPUT_JSON_FILE)

    files: list[str] = os.listdir(path)
    files.remove("W")
    files.remove("W2")
    files.remove("W3")
    files.remove("Captures")
    print(files)
    for i, file in enumerate(files):
        if (
            IGNORED_FOLDERS.__contains__(file)
            or not file.endswith(SUPPORTED_EXTENSIONS)
            and not os.path.isdir(path + file)
            and file.endswith(".html")
        ):
            files.remove(file)
        if file.rfind(".") != -1:
            files[i] = file[0 : len(file) - 4]
    args_search(files)


def pre_test():
    try:
        master_version: str = ""
        for line_str in urllib.request.urlopen(
            "https://raw.githubusercontent.com/cinemagoer/cinemagoer/master/imdb/version.py"
        ):
            master_version: str = line_str.decode("utf-8")
        master_version = (
            master_version.removeprefix("__version__ = '").replace("'", "").strip()
        )
        print(f"imdb={imdb}")
        print(f"imdb.VERSION={imdb.VERSION}, master_version={master_version}")
        if imdb.VERSION != master_version:
            print(
                "********************************************************************************************************************************"
            )
            print(
                "********************************************************************************************************************************"
            )
            print(
                "********************************************************************************************************************************"
            )
            print(f"								 UPDATE Cinemagoer to version {master_version}")
            print(
                "********************************************************************************************************************************"
            )
            print(
                "********************************************************************************************************************************"
            )
            print(
                "********************************************************************************************************************************"
            )

        cinemagoer: Cinemagoer = Cinemagoer()
        test: Movie = cinemagoer.get_movie(34583, info=["main", "awards"])
        print(
            '********************* Test to see if "awards" are still broken *********************'
        )
        print(
            f"movie['title']: {test['title']}, movie.get('awards'): {test.get('awards')}, ia.get_movie_awards(movie.movieID): {cinemagoer.get_movie_awards(test.movieID)}"
        )
        if (
            "movie['title']: Casablanca, movie.get('awards'): None, ia.get_movie_awards(movie.movieID): {'data': {}, 'titlesRefs': {}, 'namesRefs': {}}"
            != f"movie['title']: {test['title']}, movie.get('awards'): {test.get('awards')}, ia.get_movie_awards(movie.movieID): {cinemagoer.get_movie_awards(test.movieID)}"
        ):
            print(
                "********************************************************************************************************************************"
            )
            print(
                "********************************************************************************************************************************"
            )
            print(
                "********************************************************************************************************************************"
            )
            print(f'								 "awards" are changed')
            print(
                "********************************************************************************************************************************"
            )
            print(
                "********************************************************************************************************************************"
            )
            print(
                "********************************************************************************************************************************"
            )
    except Exception as ex:
        print(f"0 ERROR {ex}: Check version")


def get_config_path():
    global file_path
    file_path = "config.json"
    if os.path.exists(file_path):
        pass
    else:
        file_path = "bin/" + file_path
        if os.path.exists(file_path):
            pass
        else:
            file_path = "{HOMEDRIVE}{HOMEPATH}/Documents/NetBeansProjects/ImdbSearch/bin/config.json"
    print(f"Config path: {file_path}")
    return file_path


if __name__ == "__main__":
    start: float = datetime.timestamp(datetime.now())

    pre_test()

    file_path: str = get_config_path()
    CONFIG = json.load(open(file_path.format(**os.environ)))

    for line in CONFIG:
        CONFIG[line] = str(CONFIG[line]).replace("${", "{").format(**os.environ)
    SUPPORTED_EXTENSIONS = tuple(CONFIG["SUPPORTED_EXTENSIONS"])
    IGNORED_FOLDERS = tuple(CONFIG["IGNORED_FOLDERS"])
    THREAD_NB: int = int(CONFIG["THREAD_NB"])
    OUTPUT_JSON_FILE: str = (
        CONFIG["OUTPUT_JSON_FILE"].replace("${", "{").format(**os.environ)
    )

    print(f"sys.argv={sys.argv}")
    if len(sys.argv[1:]) > 0:
        print("Custom args.")
        args_search(sys.argv[1:])
    else:
        print("Default path.")
        # path_search(str(Path.home()) + os.sep + "Videos" + os.sep)
        # path_search(str(Path.home()) + os.sep + "Videos" + os.sep + "W" + os.sep)
        # path_search("D:/Films/W2/")
        # path_search("C:/Users/rivoi/Videos/W/Underworld")
        path_search("C:/Users/ADELE/Videos")

    sys.exit()
