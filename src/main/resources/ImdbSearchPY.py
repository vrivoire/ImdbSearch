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
from queue import Queue
from threading import Thread
from typing import Any

import jsonpickle

from imdbinfo import get_movie, search_title, get_akas
from imdbinfo.models import MovieDetail, MovieBriefInfo, SearchResult

SUPPORTED_EXTENSIONS = None
IGNORED_FOLDERS = None
OUTPUT_JSON_FILE = None
search_path = None


# https://www.geeksforgeeks.org/python/how-to-remove-string-accents-using-python-3/
def load_data(thread_index: int, path: str, title: str) -> str:
    sys.stdout.reconfigure(encoding='utf-8')
    title = title.encode('utf-8') if type(title) == bytes else title

    print(f'{thread_index} Looking for path: {path}, title: {title}')

    imdb_id: str | None = ''
    looking_year: str = ''
    *middle, last = title.split()
    if len(middle) == 0:
        looking_title: str = last.lower()
    else:
        looking_title: str = " ".join(middle).lower()
        try:
            looking_year: int = int(last)
        except ValueError as ex:
            print(f'{thread_index} ERROR year not found for {title}, {ex}')
            looking_year: str = ''
            looking_title = title

    try:
        result: SearchResult | None = search_title(title)
        if result:
            movies: list[MovieBriefInfo] = result.titles
            kind: str | None = None
            print(f'{thread_index} *** ({title}) {len(movies)}')
            for movie in movies:
                print(f'{thread_index} *** ({title}) {movie}')
                akas: list[str] = [cleanup_title(aka.title) for aka in get_akas(movie.imdb_id)['akas']]
                akas.insert(0, cleanup_title(movie.title))
                titles: set[str] = set(akas)

                if not imdb_id:
                    kind = movie.kind.lower()
                    if (
                            'podcast' not in kind
                            and 'game' not in kind
                            and 'mimi' not in kind
                            and 'vg' not in kind
                            and not movie.is_episode()
                    ):
                        print(f'{thread_index} {looking_title} --> kind: {kind}, len: {len(titles)}, found: {looking_title in titles}, {titles}')
                        if os.path.isdir(path + '/' + title) and movie.is_series():
                            imdb_id = movie.imdb_id
                            break
                        elif not os.path.isdir(path + '/' + title) and not movie.is_series() and (movie.year == looking_year):
                            imdb_id = movie.imdb_id
                            break

            if imdb_id:
                print(f'{thread_index} FOUND {imdb_id}, {kind} - looking={looking_title} {looking_year} - title={title}')
            else:
                print(f'{thread_index} NOT FOUND looking: {looking_title} {looking_year}')

    except Exception as ex:
        print(f"{thread_index} 1 ERROR {ex}: {title}")
        print(traceback.format_exc())

    return imdb_id


def cleanup_title(movie_title: str) -> str:
    return (movie_title.lower().replace('\\', '').replace('/', '').replace(':', '').replace('?', '').replace('"', '')
            .replace('<', '').replace('>', '').replace('|', '').replace('.', '').replace('  ', ' '))


def populate(thread_index: int, imdb_id: str, title: str) -> dict[str, Any]:
    try:
        if imdb_id:
            movie_imdbinfo: MovieDetail | None = get_movie(imdb_id)
            if movie_imdbinfo:
                try:
                    prop: dict[str, Any] = {
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
                        "main.year": str(movie_imdbinfo.year),
                        "main.duration": movie_imdbinfo.duration,
                        "plot.plot": [movie_imdbinfo.plot],
                        "plot.synopsis": movie_imdbinfo.synopses,
                        "main.country codes": [x.lower() for x in movie_imdbinfo.country_codes] if movie_imdbinfo.country_codes else [],
                        'is Series': movie_imdbinfo.is_series(),
                    }

                    if movie_imdbinfo.is_series():
                        prop['creators'] = list(reversed([creator.name for creator in movie_imdbinfo.info_series.get_creators()]))
                        prop['seasons'] = len(movie_imdbinfo.info_series.display_seasons)

                        year_list = list(reversed([year for year in movie_imdbinfo.info_series.display_years]))
                        if len(year_list) == 1:
                            prop["main.year"] = year_list[0]
                        else:
                            prop["main.year"] = f'{year_list[0]}...{year_list[len(year_list) - 1]}'

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
    print(f'OUTPUT_JSON_FILE: {OUTPUT_JSON_FILE}')
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


def spawn(thread_index: int, path: str, titles: list[str], result_queue: Queue):
    try:
        print(f"\tStarting thread_id: {thread_index}, path: {path}, titles: {titles}")
        size: int = len(titles)
        i: int = 0
        title: str
        for title in titles:
            i += 1
            prop: dict[str, Any] = {}
            try:
                title = title.replace(".", " ")
            except Exception:
                pass  # No dot and extension

            try:
                imdb_id: str | None = load_data(thread_index, path, title)
                prop: dict[str, Any] = populate(thread_index, imdb_id, title)
            finally:
                print(f"\t\tupdate: thread_id: {thread_index}, {i}/{size}, found: {title}")
                result_queue.put({title: prop})
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
    result_queue: Queue = Queue()
    print(f"THREAD_NB: {THREAD_NB}, file_count: {file_count}, thread_nb: {thread_nb}, files_per_thread: {files_per_thread}, remain_files: {remain_files}, file_count / thread_nb: {file_count / thread_nb}")

    threads: list[Thread] = []
    i: int = 0
    thread_id: int = 1
    for thread_id in range(1, thread_nb + 1):
        k: int = thread_id * files_per_thread
        print(f"thread_id: {thread_id}, {range(i, k)}, size: {len(files[i:k])}")
        thread: Thread = threading.Thread(
            target=spawn,
            args=(thread_id, path, files[i:k], result_queue),
            name=thread_id.__str__()
        )
        threads.append(thread)
        thread.start()
        i = k
    print(f"thread_id: {thread_nb + 1}, {range(file_count - remain_files, file_count)}, size: {len(files[file_count - remain_files:file_count])}")
    thread: Thread = threading.Thread(
        target=spawn,
        args=(thread_id + 1, path, files[file_count - remain_files: file_count], result_queue),
        name=(thread_nb + 1).__str__(),
    )
    threads.append(thread)
    thread.start()

    for thread in threads:
        thread.join()

    props: dict[str, Any] = {}
    while not result_queue.empty():
        props.update(result_queue.get())
    print("All tasks has been finished")

    for i in range(1, 5):
        for key in props.keys():
            if len(props.get(key)) == 0:
                print(f'Retrying {key}')
                imdb_id: str = load_data(0, path, key)
                prop: dict[str, Any] = populate(0, imdb_id, key)
                props.update({key: prop})

    save_json(props)
    print(f"Threads: {thread_nb}, Time elapsed: {datetime.fromtimestamp(datetime.timestamp(datetime.now()) - start).strftime('%M:%S.%f')} for {len(files)} titles, {datetime.fromtimestamp((datetime.timestamp(datetime.now()) - start) / len(files)).strftime('%M:%S.%f')} per title.")


def path_search(path):
    print(f"Searching into path: {path}")
    files: list[str] = os.listdir(path)
    print(f"Searching into files: {files}")
    print(f'IGNORED_FOLDERS: {type(IGNORED_FOLDERS)}')
    print(f'SUPPORTED_EXTENSIONS: {SUPPORTED_EXTENSIONS}')
    for folder in IGNORED_FOLDERS:
        try:
            print(f'removing {folder} {files.remove(folder)}')
        except ValueError as ve:
            pass

    OUTPUT_JSON_FILE: str = CONFIG["OUTPUT_JSON_FILE"].replace("${", "{").format(**os.environ)
    print(f'OUTPUT_JSON_FILE: {OUTPUT_JSON_FILE}')

    for i, file in enumerate(files):
        if file.rfind(".") != -1:
            if file[file.rfind(".") + 1:] in SUPPORTED_EXTENSIONS:
                files[i] = file[0: len(file) - 4]
            else:
                print(f'removing {file} {files.remove(file)}')

    print(f'files={files}')
    args_search(path, files)


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

    file_path: str = get_config_path().format(**os.environ)
    with open(file_path) as infile:
        CONFIG = json.load(infile)
    print(f'CONFIG={CONFIG}')

    SUPPORTED_EXTENSIONS = CONFIG["SUPPORTED_EXTENSIONS"]
    print(f'SUPPORTED_EXTENSIONS={SUPPORTED_EXTENSIONS}')
    IGNORED_FOLDERS = CONFIG["IGNORED_FOLDERS"]
    print(f'IGNORED_FOLDERS={IGNORED_FOLDERS}')
    THREAD_NB: int = int(CONFIG["THREAD_NB"])
    print(f'THREAD_NB={THREAD_NB}')
    OUTPUT_JSON_FILE: str = CONFIG["OUTPUT_JSON_FILE"].replace("${", "{").format(**os.environ)
    print(f'OUTPUT_JSON_FILE: {OUTPUT_JSON_FILE}')

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

        path_search("C:/Users/ADELE/Videos/W/toto")
        # path_search("C:/Users/ADELE/Videos/W")
        # path_search("C:/Users/ADELE/Videos/")

    sys.exit()
