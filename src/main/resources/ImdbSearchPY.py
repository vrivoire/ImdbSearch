# cd C:\Users\rivoi\Documents\NetBeansProjects\PycharmProjects\ImdbSearchPY
# pyinstaller --onefile main.py --icon=C:\Users\rivoi\Documents\NetBeansProjects\PycharmProjects\ImdbSearchPY\IMDb.ico --nowindowed --noconsole --paths C:\Users\rivoi\Documents\NetBeansProjects\PycharmProjects\ImdbSearchPYvenv\Lib\site-packages
# pip install git+https://github.com/cinemagoer/cinemagoer
import json
import logging as log
import logging.handlers
import os
import sys
import threading
import traceback
from datetime import datetime
from queue import Queue
from threading import Thread
from typing import Any

import jmespath
import jsonpickle
import unicodedata
from imdbinfo import get_movie, search_title, get_akas
from imdbinfo.models import MovieDetail, MovieBriefInfo, SearchResult
from imdbinfo.services import normalize_imdb_id, request_json_url

SUPPORTED_EXTENSIONS = None
IGNORED_FOLDERS = None
OUTPUT_JSON_FILE = None
search_path = None

log.basicConfig(
    level=logging.INFO,
    format="[%(levelname)-8s] [%(filename)s.%(funcName)s:%(lineno)d] %(message)s",
    handlers=[
        logging.StreamHandler()
    ]
)


def remove_accents(text):
    return "".join(c for c in unicodedata.normalize("NFD", text) if not unicodedata.combining(c))


# https://www.geeksforgeeks.org/python/how-to-remove-string-accents-using-python-3/
def load_data(thread_index: int, path: str, title: str) -> str | None:
    sys.stdout.reconfigure(encoding='utf-8')
    title = title.encode('utf-8') if type(title) == bytes else title
    title = title.lower()
    log.info(f'\t\tid: {thread_index} Looking for path: {path}, title: {title}')

    imdb_id: str | None = None
    looking_year: str = ''
    *middle, last = title.split()
    if len(middle) == 0:
        looking_title: str = last
    else:
        looking_title: str = " ".join(middle).lower()
        try:
            looking_year: int = int(last)
        except ValueError as ex:
            log.warning(f'\t\tid: {thread_index} year not found for {title}, {ex}')
            looking_year: str = ''
            looking_title = title
    log.info(f'\t\tid: {thread_index} {looking_title} {looking_year}')

    try:
        search_result: SearchResult | None = search_title(title)
        if search_result:
            movies: list[MovieBriefInfo] = search_result.titles
            kind: str | None = None
            log.info(f'\t\tid: {thread_index} {title}: len: {len(movies)}, {movies}')
            for movie in movies:
                log.info(f'\t\tid: {thread_index} {title}: {movie}')
                akas: list[str] = [cleanup_title(aka.title) for aka in get_akas(movie.imdb_id)['akas']]
                akas.insert(0, cleanup_title(movie.title))
                titles: set[str] = set(akas)
                log.info(f'\t\tid: {thread_index} {title}: AKAS: {titles}')

                if not imdb_id:
                    kind = movie.kind.lower()
                    log.info(f'\t\tid: {thread_index} {title}: {kind}, is_series: {movie.is_series()}, is_episode: {movie.is_episode()}')
                    if (
                            'podcast' not in kind
                            and 'game' not in kind
                            # and 'mini' not in kind
                            and 'vg' not in kind
                            and not movie.is_episode()
                    ):
                        log.info(f'\t\tid: {thread_index} {looking_title} --> kind: {kind}, is_series: {movie.is_series()}, len: {len(titles)}, found: {looking_title in titles}, {titles}')
                        found_year = looking_year != '' and movie.year == looking_year
                        # log.info(f'found_year={found_year}')
                        r_accentes = remove_accents(looking_title)
                        log.info(f'looking_title = {looking_title}, r_accentes = {r_accentes}')
                        # log.info(f'toto1={remove_accents(t) for t in titles}')
                        # log.info(f'toto1={r_accentes in [remove_accents(t) for t in titles]}')
                        if r_accentes in [remove_accents(t) for t in titles]:
                            if found_year and (movie.year == looking_year):
                                # log.info(1)
                                if os.path.isdir(path + '/' + title) and movie.is_series():
                                    # log.info(2)
                                    imdb_id = movie.imdb_id
                                    break
                                elif not os.path.isdir(path + '/' + title) and not movie.is_series():
                                    # log.info(3)
                                    imdb_id = movie.imdb_id
                                    break
                            else:
                                # log.info(4)
                                if os.path.isdir(path + '/' + title) and movie.is_series():
                                    # log.info(5)
                                    imdb_id = movie.imdb_id
                                    break
                                elif not os.path.isdir(path + '/' + title) and not movie.is_series():
                                    # log.info(6)
                                    imdb_id = movie.imdb_id
                                    break

            if imdb_id:
                log.info(f'\t\tid: {thread_index} FOUND {imdb_id}, {kind} - looking={looking_title} {looking_year} - title={title}')
            else:
                log.info(f'\t\tid: {thread_index} NOT FOUND looking: {looking_title} {looking_year}')

    except Exception as ex:
        log.error(f"\t\tid: {thread_index} 1 ERROR {ex}: {title}")
        log.error(traceback.format_exc())
        if ex.__str__().find('****** AWS WAF enforcement in place. Try again later. ******') != -1:
            return None
    return imdb_id


def cleanup_title(movie_title: str) -> str:
    return (movie_title.lower().replace('\\', '').replace('/', '').replace(':', '').replace('?', '').replace('"', '')
            .replace('<', '').replace('>', '').replace('|', '').replace('.', '').replace('  ', ' '))


# https://github.com/tveronesi/imdbinfo/issues/141
def get_fr_plot(imdb_id: str) -> str:
    plots: set[str] = set()
    imdb_id, lang = normalize_imdb_id(imdb_id)
    raw_json: dict = request_json_url(f"https://www.imdb.com/{lang}/title/tt{imdb_id}/reference")
    result = jmespath.search('props.pageProps.mainColumnData.plot.plotText.plainText', raw_json)
    plots.add(result.replace('"', "'")) if result is not None else None
    for locale in raw_json['locales']:
        if locale.rfind('fr') != -1:
            imdb_id, lang = normalize_imdb_id(imdb_id, locale.lower())
            raw_json_fr: dict = request_json_url(f"https://www.imdb.com/{lang}/title/tt{imdb_id}/reference")
            result = jmespath.search('props.pageProps.mainColumnData.plot.plotText.plainText', raw_json_fr)
            plots.add(result.replace('"', "'")) if result is not None else None

    plot_list = sorted(list(plots))
    plot_list_str: str = ''
    for plot in plot_list:
        plot_list_str = f'{plot_list_str}{plot}\n\n'
    plot_list_str = plot_list_str[0:len(plot_list_str) - 2] if len(plot_list_str) > 0 else None
    return plot_list_str


def populate(thread_index: int, imdb_id: str|None, title: str) -> dict[str, Any]:
    try:
        if imdb_id:
            movie_detail: MovieDetail | None = get_movie(imdb_id)
            if movie_detail:
                try:
                    prop: dict[str, Any] = {
                        "main.imdbID": imdb_id,
                        'main.Imdbid': imdb_id,
                        "main.title": movie_detail.title,
                        "main.votes": movie_detail.votes,
                        "main.genres": movie_detail.genres if movie_detail.genres else [],
                        "main.language codes": movie_detail.languages if movie_detail.languages else [],
                        "main.aspect ratio": movie_detail.aspect_ratios[0][0] if movie_detail.aspect_ratios else '',
                        "main.cover url": movie_detail.cover_url,
                        "main.kind": movie_detail.kind,
                        "main.rating": movie_detail.rating if movie_detail.rating else 0.0,
                        "main.year": str(movie_detail.year),
                        "main.duration": movie_detail.duration,
                        "plot.plot": get_fr_plot(imdb_id),
                        "plot.synopsis": movie_detail.synopses,
                        "main.country codes": [x.lower() for x in movie_detail.country_codes] if movie_detail.country_codes else [],
                        'is Series': movie_detail.is_series(),
                    }
                    if movie_detail.awards:
                        prop['main.awards.wins'] = movie_detail.awards.wins
                        prop['main.awards.nominations'] = movie_detail.awards.nominations
                        prop['main.awards.prestigious_award'] = movie_detail.awards.prestigious_award

                    if movie_detail.is_series():
                        prop['creators'] = list(reversed([creator.name for creator in movie_detail.info_series.get_creators()]))
                        prop['seasons'] = len(movie_detail.info_series.display_seasons)

                        year_list = list(reversed([year for year in movie_detail.info_series.display_years]))
                        if len(year_list) == 0:
                            pass
                        elif len(year_list) == 1:
                            prop["main.year"] = year_list[0]
                            prop["main.years"] = ''
                        else:
                            prop["main.year"] = year_list[0]
                            prop["main.years"] = f'{year_list[0]}...{year_list[len(year_list) - 1]}'

                    if movie_detail.categories:
                        prop['main.writers'] = [writer.name for writer in movie_detail.categories.get('writer')] if movie_detail.categories.get('writer') else []
                        prop["main.directors"] = [director.name for director in movie_detail.categories.get('director')] if movie_detail.categories.get('director') else []
                        prop["main.casts"] = [star.name for star in movie_detail.stars] if movie_detail.categories.get('cast') else []

                    return prop

                except Exception as ex:
                    log.error(f"\t\tid: {thread_index} 5 ERROR: {imdb_id}, {title} --> {ex}")
                    log.error(traceback.format_exc())
    except Exception as ex:
        log.error(f"\t\tid: {thread_index} 1.1 ERROR title={title}, movieID={imdb_id}, msg={ex.__str__()}")
        log.error(traceback.format_exc())
    return {}


def save_json(prop: dict[str, Any]) -> None:
    log.info(f'OUTPUT_JSON_FILE: {OUTPUT_JSON_FILE}')
    if os.path.isfile(OUTPUT_JSON_FILE):
        log.info(f"Deleting {OUTPUT_JSON_FILE}...")
        os.remove(OUTPUT_JSON_FILE)

    log.info(f"Writing file {OUTPUT_JSON_FILE}")
    with open(OUTPUT_JSON_FILE, "w", encoding="utf-8") as outfile:
        try:
            json_str: str = json.dumps(prop, indent=4, sort_keys=True)
            if len(json_str) == 0:
                log.info("json_str is empty, trying with jsonpickle.")
                json_str = jsonpickle.encode(prop, indent=4)
            outfile.write(json_str)
            # log.info(json_str)
        except Exception as ex:
            log.error(f"5 ERROR: {ex}\n{prop}")
            log.error(traceback.format_exc())


def spawn(thread_index: int, path: str, titles: list[str], result_queue: Queue):
    try:
        log.error(f"\tid: {thread_index} Starting thread_id: {thread_index}, path: {path}, titles: {titles}")
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
                log.info(f"\t\tid: {thread_index} {i}/{size}, found: {title}")
                result_queue.put({title: prop})
        log.info(f"\tid: {thread_index} Ending {thread_index}")
    except Exception as ex:
        log.error(f"\tid: {thread_index} 7 ERROR - ************** IMDbError ************** thread_id: {thread_index}, {ex}: {titles}")
        log.error(traceback.format_exc())


def args_search(path: str, files: list[str]):
    now: datetime = datetime.now()
    file_count: int = len(files)
    log.info(f"args_search: {path}, {len(files)} files, files: {files}")

    if file_count <= THREAD_NB:
        thread_nb: int = file_count
        files_per_thread: int = 1
    else:
        thread_nb: int = THREAD_NB
        files_per_thread: int = int(file_count / thread_nb)

    remain_files: int = file_count - files_per_thread * thread_nb
    result_queue: Queue = Queue()
    log.info(f"THREAD_NB: {THREAD_NB}, file_count: {file_count}, thread_nb: {thread_nb}, files_per_thread: {files_per_thread}, remain_files: {remain_files}, file_count / thread_nb: {file_count / thread_nb}")

    threads: list[Thread] = []
    i: int = 0
    thread_id: int = 1
    for thread_id in range(1, thread_nb + 1):
        k: int = thread_id * files_per_thread
        log.info(f"thread_id: {thread_id}, {range(i, k)}, size: {len(files[i:k])}")
        thread: Thread = threading.Thread(
            target=spawn,
            args=(thread_id, path, files[i:k], result_queue),
            name=thread_id.__str__()
        )
        threads.append(thread)
        thread.start()
        i = k
    if len(files[file_count - remain_files:file_count]) > 0:
        log.info(f"thread_id: {thread_nb + 1}, {range(file_count - remain_files, file_count)}, size: {len(files[file_count - remain_files:file_count])}")
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
    log.info("All tasks has been finished")

    for i in range(1, 5):
        for title in props.keys():
            if len(props.get(title)) == 0:
                log.info(f'Retrying {title}')
                imdb_id: str|None = load_data(0, path, title)
                prop: dict[str, Any] = populate(0, imdb_id, title)
                props.update({title: prop})

    save_json(props)
    elapsed = datetime.now().now() - now
    log.info(f"Threads: {thread_nb}, Time elapsed: {elapsed} for {len(files)} titles, {elapsed / len(files)} per title.")


def path_search(path):
    log.info(f"Searching into path: {path}")
    files: list[str] = os.listdir(path)
    log.info(f"Searching into files: {files}")
    log.info(f'IGNORED_FOLDERS: {IGNORED_FOLDERS}')
    log.info(f'SUPPORTED_EXTENSIONS: {SUPPORTED_EXTENSIONS}')
    for folder in IGNORED_FOLDERS:
        try:
            log.info(f'removing {folder} {files.remove(folder)}')
        except ValueError as ve:
            pass

    OUTPUT_JSON_FILE: str = CONFIG["OUTPUT_JSON_FILE"].replace("${", "{").format(**os.environ)
    log.info(f'OUTPUT_JSON_FILE: {OUTPUT_JSON_FILE}')

    for i, file in enumerate(files):
        if file.rfind(".") != -1:
            if file[file.rfind(".") + 1:] in SUPPORTED_EXTENSIONS:
                files[i] = file[0: len(file) - 4]
            else:
                log.info(f'removing {file} {files.remove(file)}')

    log.info(f'files={files}')
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
    log.info(f"Config path: {file_path}")
    return file_path


if __name__ == "__main__":
    file_path: str = get_config_path().format(**os.environ)
    with open(file_path) as infile:
        CONFIG = json.load(infile)
    log.info(f'CONFIG={CONFIG}')

    SUPPORTED_EXTENSIONS = CONFIG["SUPPORTED_EXTENSIONS"]
    log.info(f'SUPPORTED_EXTENSIONS={SUPPORTED_EXTENSIONS}')
    IGNORED_FOLDERS: list[str] = CONFIG["IGNORED_FOLDERS"]
    log.info(f'IGNORED_FOLDERS={IGNORED_FOLDERS}')
    THREAD_NB: int = int(CONFIG["THREAD_NB"])
    log.info(f'THREAD_NB={THREAD_NB}')
    OUTPUT_JSON_FILE: str = CONFIG["OUTPUT_JSON_FILE"].replace("${", "{").format(**os.environ)
    log.info(f'OUTPUT_JSON_FILE: {OUTPUT_JSON_FILE}')

    log.info(f"sys.argv={sys.argv}")
    if len(sys.argv[1:]) > 0:
        log.info(f"Custom args {sys.argv[1:]}")
        args_search(sys.argv[1], sys.argv[2:])
    else:
        log.info("Default path.")
        # path_search("C:/Users/ADELE/Videos")
        # path_search("C:/Users/ADELE/Videos/W")
        # path_search("C:/Users/ADELE/Videos/W2")
        # path_search("C:/Users/ADELE/Videos/W3")
        path_search("C:/Users/ADELE/Videos/W4")
        # path_search("C:/Users/ADELE/Videos/W/Kaamelott")

        if os.path.isfile(OUTPUT_JSON_FILE):
            with open(OUTPUT_JSON_FILE, 'r', encoding='utf-8') as file:
                print(json.dumps(json.load(file), indent=4))

    sys.exit()
