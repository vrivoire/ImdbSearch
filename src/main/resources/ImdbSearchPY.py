# cd C:\Users\rivoi\Documents\NetBeansProjects\PycharmProjects\ImdbSearchPY
# pyinstaller --onefile main.py --icon=C:\Users\rivoi\Documents\NetBeansProjects\PycharmProjects\ImdbSearchPY\IMDb.ico --nowindowed --noconsole --paths C:\Users\rivoi\Documents\NetBeansProjects\PycharmProjects\ImdbSearchPYvenv\Lib\site-packages
# pip install git+https://github.com/cinemagoer/cinemagoer

import json
import os
import sys
import threading
import traceback
import urllib.request
from datetime import datetime
from threading import Thread
from typing import Any

import imdb
import jsonpickle

from imdbinfo import get_movie, search_title, get_akas
from imdbinfo.models import MovieDetail, MovieBriefInfo, SearchResult

SUPPORTED_EXTENSIONS = None
IGNORED_FOLDERS = None
search_path = None
props: dict[str, Any] = {}


# https://www.geeksforgeeks.org/python/how-to-remove-string-accents-using-python-3/
def load_data(path: str, title: str) -> str:
    sys.stdout.reconfigure(encoding='utf-8')
    title = title.encode('utf-8') if type(title) == bytes else title

    print(f'Looking for path: {path}, title: {title}')

    imdb_id: str | None = ''
    try:
        result: SearchResult | None = search_title(title)
        if result:
            movies: list[MovieBriefInfo] = result.titles
            kind: str | None = None
            looking_title: str | None = None
            looking_year: int | None = None

            for movie in movies:
                akas: list[str] = [cleanup_title(aka.title) for aka in get_akas(movie.imdb_id)['akas']]
                akas.insert(0, cleanup_title(movie.title))
                titles: set[str] = set(akas)

                if not imdb_id:
                    kind = movie.kind.lower()
                    *middle, last = title.split()
                    if len(middle) == 0:
                        looking_title = last.lower()
                    else:
                        looking_title = " ".join(middle).lower()
                        try:
                            looking_year = int(last)
                        except Exception as ex:
                            print(f'ERROR year not found for {title}, {ex}')
                            looking_year = None

                    if looking_title in titles:
                        if (
                                'podcast' not in kind
                                and 'game' not in kind
                                and 'mimi' not in kind
                                and 'vg' not in kind
                                and not movie.is_episode()
                        ):
                            print(f'{looking_title} --> kind: {kind}, len: {len(titles)}, found: {looking_title in titles}, {titles}')
                            if os.path.isdir(path + '/' + title) and movie.is_series():
                                imdb_id = movie.imdb_id
                                break
                            elif not os.path.isdir(path + '/' + title) and not movie.is_series() and movie.year == looking_year:
                                imdb_id = movie.imdb_id
                                break

            # print(f'{kind} - looking={looking_title} {looking_year} - title={movie.title.lower()} {movie.year}')
            if imdb_id:
                print(f'FOUND {imdb_id}, {kind} - looking={looking_title} {looking_year} - title={title}')
            else:
                print(f'NOT FOUND looking={looking_title} {looking_year}')

    except Exception as ex:
        print(f"1 ERROR {ex}: {title}")
        print(traceback.format_exc())

    return imdb_id


def cleanup_title(movie_title: str) -> str:
    return (movie_title.lower().replace('\\', '').replace('/', '').replace(':', '').replace('?', '').replace('"', '')
            .replace('<', '').replace('>', '').replace('|', '').replace('.', '').replace('  ', ' '))


def populate(imdb_id: str, title: str):
    try:
        if imdb_id:
            movie_imdbinfo: MovieDetail | None = get_movie(imdb_id)
            if movie_imdbinfo:
                try:
                    prop: dict = {
                        "main.imdbID": imdb_id,
                        'main.Imdbid': imdb_id,
                        "main.title": movie_imdbinfo.title,
                        "main.votes": movie_imdbinfo.votes,
                        "main.genres": movie_imdbinfo.genres if movie_imdbinfo.genres else [],
                        "main.language codes": movie_imdbinfo.languages if movie_imdbinfo.languages else [],
                        "main.aspect ratio": movie_imdbinfo.aspect_ratios[0][0] if movie_imdbinfo.aspect_ratios else '',
                        "main.cover url": movie_imdbinfo.cover_url,
                        "main.kind": movie_imdbinfo.kind,
                        "main.rating": movie_imdbinfo.rating if movie_imdbinfo.rating else 0.0,
                        "main.year": movie_imdbinfo.year,
                        "plot.plot": [movie_imdbinfo.plot],
                        "plot.synopsis": movie_imdbinfo.synopses,
                        "main.country codes": [x.lower() for x in movie_imdbinfo.country_codes] if movie_imdbinfo.country_codes else []
                    }
                    if movie_imdbinfo.categories:
                        prop["main.writers"] = (
                            [writer.name for writer in movie_imdbinfo.categories.get("writer")]
                            if movie_imdbinfo.categories.get("writer")
                            else []
                        )
                        prop["main.directors"] = (
                            [director.name for director in movie_imdbinfo.categories.get("director")]
                            if movie_imdbinfo.categories.get("director")
                            else []
                        )
                        prop["main.casts"] = (
                            [star.name for star in movie_imdbinfo.stars]
                            if movie_imdbinfo.categories.get("cast")
                            else []
                        )

                    if movie_imdbinfo.categories:
                        prop['main.writers'] = [writer.name for writer in movie_imdbinfo.categories.get('writer')] if movie_imdbinfo.categories.get('writer') else []
                        prop["main.directors"] = [director.name for director in movie_imdbinfo.categories.get('director')] if movie_imdbinfo.categories.get('director') else []
                        prop["main.casts"] = [star.name for star in movie_imdbinfo.stars] if movie_imdbinfo.categories.get('cast') else []

                    return prop

                except Exception as ex:
                    print(f"5 ERROR: {imdb_id}, {title} --> {ex}")
                    print(traceback.format_exc())
    except Exception as ex:
        print(f"1.1 ERROR title={title}, movieID={imdb_id}, msg={ex.__str__()}")
        print(traceback.format_exc())
    return {}


def save_json(prop: dict[str, Any]) -> None:
    if os.path.isfile(OUTPUT_JSON_FILE):
        print(f"Deleting {OUTPUT_JSON_FILE}...")
        os.remove(OUTPUT_JSON_FILE)

    print(f"Writing file {OUTPUT_JSON_FILE}")
    with open(OUTPUT_JSON_FILE, "w", encoding="utf-8") as outfile:
        try:
            json_str: str = json.dumps(prop, indent=4, sort_keys=True)
            if len(json_str) == 0:
                print("json_str is empty, trying with jsonpickle.")
                json_str = jsonpickle.encode(prop, indent=4)
            outfile.write(json_str)
        except Exception as ex:
            print(f"5 ERROR: {ex}\n{prop}")
            print(traceback.format_exc())


def spawn(thread_index: int, path: str, titles: list[str]):
    try:
        print(f"\tStarting thread_id: {thread_index}, path: {path}, titles: {titles}")
        size: int = len(titles)
        i: int = 0
        global props
        title: str
        for title in titles:
            i += 1
            prop: dict[str, Any] = {}
            try:
                title = title.replace(".", " ")
            except Exception:
                pass  # No dot and extension

            try:
                imdb_id: str | None = load_data(path, title)
                prop = populate(imdb_id, title)
            finally:
                print(f"\t\tupdate: thread_id: {thread_index}, {i}/{size}, found: {title}")
                props.update({title: prop})
        print(f"\tEnding {thread_index}\r")
    except Exception as ex:
        print(f"\t7 ERROR - ************** IMDbError ************** thread_id: {thread_index}, {ex}: {titles}")
        print(traceback.format_exc())


def args_search(path: str, files: list[str]):
    file_count: int = len(files)
    print(f"args_search: {path}, {len(files)} files, files: {files}")

    if file_count <= THREAD_NB:
        thread_nb: int = file_count
        files_per_thread: int = 1
    else:
        thread_nb: int = THREAD_NB
        files_per_thread: int = int(file_count / thread_nb)

    remain_files: int = file_count - files_per_thread * thread_nb

    print(f"THREAD_NB: {THREAD_NB}, file_count: {file_count}, thread_nb: {thread_nb}, files_per_thread: {files_per_thread}, remain_files: {remain_files}, file_count / thread_nb: {file_count / thread_nb}")

    threads: list[Thread] = []
    i: int = 0
    thread_id: int = 1
    for thread_id in range(1, thread_nb + 1):
        k: int = thread_id * files_per_thread
        print(f"thread_id: {thread_id}, {range(i, k)}, size: {len(files[i:k])}")
        thread: Thread = threading.Thread(
            target=spawn,
            args=(thread_id, path, files[i:k]),
            name=thread_id.__str__()
        )
        threads.append(thread)
        thread.start()
        i = k
    print(f"thread_id: {thread_nb + 1}, {range(file_count - remain_files, file_count)}, size: {len(files[file_count - remain_files:file_count])}")
    thread: Thread = threading.Thread(
        target=spawn,
        args=(thread_id + 1, path, files[file_count - remain_files: file_count]),
        name=(thread_nb + 1).__str__(),
    )
    threads.append(thread)
    thread.start()

    for thread in threads:
        thread.join()
    print("All tasks has been finished")

    print(f'Start retrying -----------------------------------------------')
    for i in range(1, 5):
        for key in props.keys():
            if len(props.get(key)) == 0:
                print(f'Retrying {key}')
                imdb_id: str = load_data(path, key)
                prop: dict = populate(imdb_id, key)
                props.update({key: prop})
    print(f'Ended retrying -----------------------------------------------')

    save_json(props)
    print(f"Threads: {thread_nb}, Time elapsed: {datetime.fromtimestamp(datetime.timestamp(datetime.now()) - start).strftime('%M:%S.%f')} for {len(files)} titles, {datetime.fromtimestamp((datetime.timestamp(datetime.now()) - start) / len(files)).strftime('%M:%S.%f')} per title.")


def path_search(path):
    print(f"Searching into path: {path}")
    files: list[str] = os.listdir(path)
    print(f"Searching into files: {files}")
    print(f'IGNORED_FOLDERS: {IGNORED_FOLDERS}')
    print(f'SUPPORTED_EXTENSIONS: {SUPPORTED_EXTENSIONS}')
    for i, file in enumerate(files):
        _, file_extension = os.path.splitext(file)
        if (
                (
                        os.path.isdir(f'{path}/{file}')
                        and file in IGNORED_FOLDERS
                )
                or file_extension[1:] not in SUPPORTED_EXTENSIONS
        ):
            files.remove(file)
            print(f'removed {file}')

    for i, file in enumerate(files):
        if file.rfind(".") != -1:
            files[i] = file[0: len(file) - 4]
    args_search(path, files)


def pre_test():
    try:
        master_version: str = ""
        for line_str in urllib.request.urlopen("https://raw.githubusercontent.com/cinemagoer/cinemagoer/master/imdb/version.py"):
            master_version: str = line_str.decode("utf-8")
        master_version = (master_version.removeprefix("__version__ = '").replace("'", "").strip())
        print(f"imdb={imdb}")
        print(f"imdb.VERSION={imdb.VERSION}, master_version={master_version}")
        if imdb.VERSION != master_version:
            print(
                "**********************************************************************************************"
            )
            print(
                "**********************************************************************************************"
            )
            print(
                "**********************************************************************************************"
            )
            print(f"					 UPDATE Cinemagoer to version {master_version}")
            print(
                "**********************************************************************************************"
            )
            print(
                "**********************************************************************************************"
            )
            print(
                "**********************************************************************************************"
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
    # print(CONFIG)
    for line in CONFIG:
        CONFIG[line] = str(CONFIG[line]).replace("${", "{").format(**os.environ)

    SUPPORTED_EXTENSIONS = CONFIG["SUPPORTED_EXTENSIONS"]
    IGNORED_FOLDERS = CONFIG["IGNORED_FOLDERS"]
    THREAD_NB: int = int(CONFIG["THREAD_NB"])

    OUTPUT_JSON_FILE: str = (
        CONFIG["OUTPUT_JSON_FILE"].replace("${", "{").format(**os.environ)
    )

    print(f"sys.argv={sys.argv}")
    if len(sys.argv[1:]) > 0:
        print(f"Custom args {sys.argv[1:]}")
        args_search(sys.argv[1], sys.argv[2:])
    else:
        print("Default path.")
        # path_search(str(Path.home()) + os.sep + "Videos" + os.sep)
        # path_search(str(Path.home()) + os.sep + "Videos" + os.sep + "W" + os.sep)
        # path_search("D:/Films/W2/")
        # path_search("C:/Users/rivoi/Videos/W/Underworld")

        path_search("C:/Users/ADELE/Videos/W")
        # path_search("C:/Users/ADELE/Videos/")

    sys.exit()
