# cd C:\Users\rivoi\Documents\NetBeansProjects\PycharmProjects\ImdbSearchPY
# pyinstaller --onefile main.py --icon=C:\Users\rivoi\Documents\NetBeansProjects\PycharmProjects\ImdbSearchPY\IMDb.ico --nowindowed --noconsole --paths C:\Users\rivoi\Documents\NetBeansProjects\PycharmProjects\ImdbSearchPYvenv\Lib\site-packages
# pip install git+https://github.com/cinemagoer/cinemagoer

import glob
import json
import os
import sys
import threading
import traceback
import urllib.request
from datetime import datetime
from typing import Any

import imdb
import jsonpickle
from imdb import Cinemagoer, Company, Movie, Person, IMDbError, IMDbDataAccessError

from imdbinfo.models import MovieDetail
from imdbinfo.services import get_movie

SUPPORTED_EXTENSIONS = None
IGNORED_FOLDERS = None
search_path = None
props: dict[str, Any] = {}


def get_movie_info(ia, movie_id: str, title):
    movie = ia.get_movie(movie_id, info=["main", "plot", "synopsis"])
    kind: str = movie.get("kind").lower()
    looking_year: int | None = None
    try:
        first, *middle, last = title.split()
        looking_year = int(last)
    except Exception as ex:
        print(f'get_year ERROR {title} {ex}')
        looking_year = None

    return kind, looking_year, movie


# https://www.imdb.com/title/tt9561862/reference/
def load_data(path: str, title: str) -> dict[str, None | list | tuple | dict | list]:
    prop: dict[str, None | list | tuple | dict | list | str] = {}
    movie: Movie.Movie | None = None
    ia: Cinemagoer = Cinemagoer()
    try:
        try:
            movies: list[Movie.Movie] = ia.search_movie(title)
            if len(movies) == 0:
                print(f"{title}: movies are empty")
            else:
                print(f"{title} ---> {[movie3['title'] for movie3 in movies]}")

                for movie in movies:
                    found: bool = False
                    kind, looking_year, movie = get_movie_info(ia, movie.movieID, title)

                    #  'video game'  ('TV', 'V', 'mini', 'VG', 'TV movie', 'TV series', 'short')
                    if len([item for item in ["podcast", "podcast episode", "game", 'mini', 'VG'] if kind in item]) == 0:
                        if os.path.isdir(path + '/' + title) and kind.find("tv") != -1 or (kind.find("movie") != -1 and len(glob.glob(f'{path + '/' + title}/*-00?.mkv'))) != 0:
                            print(f"\t\t\t0 Looking for '{title}' {looking_year} --> {movie.get("kind")}")
                            if looking_year and looking_year > 1800 and looking_year == movie.get('year'):
                                print(f"\t\t\t1 Found for '{title}' {looking_year} --> {kind} and {movie.movieID}")
                                kind, looking_year, movie = get_movie_info(ia, movie.movieID, title)
                                print(f"\t\t\t2 Looking for '{title}' {looking_year} --> {kind}")
                                if len([item for item in ["podcast", "podcast episode", "game", 'mini', 'VG'] if kind in item]) == 0:
                                    if os.path.isdir(path + '/' + title) and kind.find("tv") != -1 or (kind.find("movie") != -1 and len(glob.glob(f'{path + '/' + title}/*-00?.mkv'))) != 0:
                                        if looking_year and looking_year > 1800 and looking_year == movie.get('year'):
                                            found = True
                                            print(f"\t\t\t2 Found for '{title}' {looking_year} --> {kind} and {movie.movieID} {found}")
                                            break

                        elif not os.path.isdir(path + title) and kind.find("movie") != -1 or kind.find("short") != -1 and kind.find("podcast") == -1:
                            print(f"\t\t\t3 Looking for '{title}' {looking_year} --> {kind}")
                            if looking_year and looking_year > 1800 and looking_year == movie.get('year'):
                                print(f"\t\t\t3 Found for '{title}' {looking_year} --> {kind} and {movie.movieID}")
                                kind, looking_year, movie = get_movie_info(ia, movie.movieID, title)
                                print(f"\t\t\t4 Looking for '{title}' {looking_year} --> {kind}")
                                if len([item for item in ["podcast", "podcast episode", "game", 'mini', 'VG'] if kind in item]) == 0:
                                    if looking_year and looking_year > 1800 and looking_year == movie.get('year'):
                                        if not os.path.isdir(path + title) and kind.find("movie") != -1 or kind.find("short") != -1 and kind.find("podcast") == -1 and movie.get('title').lower().find('podcast') == -1:
                                            found = True
                                            print(f"\t\t\t4 Found for '{title}' {looking_year} --> {kind} and {movie.movieID} {found}")
                                            break
                        if found:
                            print(f"\t1FOUND *---> title={title}, movieID={movie.movieID}, title={movie.get('title')}, year={movie.get('year')}, kind={movie.get('kind')}, rating={movie.get('rating')}")
                            break
                    else:
                        found = False
                        movie = None

                    if found:
                        print(f"\t2FOUND *---> title={title}, movieID={movie.movieID}, title={movie.get('title')}, year={movie.get('year')}, kind={movie.get('kind')}, rating={movie.get('rating')}")
                    else:
                        print(f"\tNOT FOUND *---> title={title}")
                        movie = None

        except IMDbDataAccessError as iex:
            print(f"1.0 ERROR title={title}, movieID={movie.movieID}, msg={iex.__str__()}")
            iex_args = dict(tuple(iex.args)[0])
            if iex_args.get('exception type') == 'IOError' and iex_args.get('original exception').__str__() == "HTTP Error 503: Service Unavailable":
                print('-------------------------------------------------------------------------------------')
                print(f'                             IMDB is kaput "{title}"')
                print(iex)
                print('-------------------------------------------------------------------------------------')
            print(traceback.format_exc())
            return {}

        except Exception as ex:
            print(f"1.1 ERROR title={title}, movieID={movie.movieID}, msg={ex.__str__()}")
            print(traceback.format_exc())
    except IMDbError as ex:
        print(f"2 ERROR {ex}: {title}")
        print(traceback.format_exc())
        raise ex

    if not movie:
        print(f"\t3 ERROR - ************** IMDbError ************** Not found: {title}")

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

        prop["main.Imdbid"] = movie.movieID
        prop["main.imdbID"] = movie.movieID
        try:
            movie2: MovieDetail | None = get_movie(movie.movieID)
            prop["main.title"] = movie2.title
            prop["main.votes"] = movie2.votes
            prop["main.genres"] = movie2.genres if movie2.genres else []
            if movie2.categories:
                # print(f'2************************* {movie2.categories.get('director')}')
                prop["main.writers"] = (
                    [writer.name for writer in movie2.categories.get("writer")]
                    if movie2.categories.get("writer")
                    else []
                )
                prop["main.directors"] = (
                    [director.name for director in movie2.categories.get("director")]
                    if movie2.categories.get("director")
                    else []
                )
                prop["main.casts"] = (
                    [star.name for star in movie2.stars]
                    if movie2.categories.get("cast")
                    else []
                )

            prop["main.aspect ratio"] = (
                movie2.aspect_ratios[0][0] if movie2.aspect_ratios else ""
            )
            prop["main.language codes"] = movie2.languages if movie2.languages else []

            prop["main.Imdbid"] = movie.movieID
            prop["main.imdbID"] = movie.movieID
            try:
                movie2: MovieDetail | None = get_movie(movie.movieID)
                prop["main.title"] = movie2.title
                prop["main.votes"] = movie2.votes
                prop["main.genres"] = movie2.genres if movie2.genres else []
                if movie2.categories:
                    # print(f'2************************* {movie2.categories.get('director')}')
                    prop['main.writers'] = [writer.name for writer in movie2.categories.get('writer')] if movie2.categories.get('writer') else []
                    prop["main.directors"] = [director.name for director in movie2.categories.get('director')] if movie2.categories.get('director') else []
                    prop["main.casts"] = [star.name for star in movie2.stars] if movie2.categories.get('cast') else []

                prop["main.aspect ratio"] = movie2.aspect_ratios[0][0] if movie2.aspect_ratios else ''
                prop["main.language codes"] = movie2.languages if movie2.languages else []

            except Exception as ex:
                print(f"4 ERROR: {movie.movieID}, {title} --> {ex}")
                print(traceback.format_exc())
        except Exception as ex:
            print(f"5 ERROR: {movie.movieID}, {title} --> {ex}")
            print(traceback.format_exc())

        for key in list(prop.keys()):
            if type(prop.get(key)) is imdb.Movie.Movie:
                try:
                    print(f"{title} -> removing {key} prop because not serializable.")
                    prop.pop(key)
                except Exception as ex:
                    pass

    return prop


def save_json(prop: dict[str, Any]) -> None:
    # if not prop or bool(prop):
    if os.path.isfile(OUTPUT_JSON_FILE):
        print(f"Deleting {OUTPUT_JSON_FILE}...")
        os.remove(OUTPUT_JSON_FILE)

    print(f"Writing file {OUTPUT_JSON_FILE}")
    print()
    with open(OUTPUT_JSON_FILE, "w", encoding="utf-8") as outfile:
        try:
            json_str: str = json.dumps(prop, indent=4, sort_keys=True)
            if len(json_str) == 0:
                print("json_str is empty, trying with jsonpickle.")
                json_str = jsonpickle.encode(prop, indent=4)
            outfile.write(json_str)
        except Exception as ex:
            print(f"5 ERROR: {ex}")
            print(f"5 ERROR: {prop}")
            print(traceback.format_exc())
    # else:
    #     # sys.exit(-1)
    #     pass


def spawn(thread_index: int, path: str, titles: list[str]):
    try:
        print(f"\tStarting thread_id: {thread_index}, path: {path}, titles: {titles}")
        size: int = len(titles)
        i: int = 0
        global props
        title: str
        for title in titles:
            i += 1
            prop: dict[str, None | list | tuple | dict | list] | None = {}
            try:
                title = title.replace(".", " ")
            except Exception:
                pass  # No dot and extension
            finally:
                try:
                    prop = load_data(path, title)
                except Exception:
                    try:
                        prop = load_data(path, title)
                    except Exception as ex:
                        print(
                            f"\t6 ERROR - ************** IMDbError ************** thread_id: {thread_index}, {ex}: {title}"
                        )
                        print(traceback.format_exc())
                finally:
                    print(f"\t\tupdate: thread_id: {thread_index}, {i}/{size}, found: {title}")
                    props.update({title: prop})
        print(f"\tEnding {thread_index}\r")
    except Exception as ex:
        print(f"\t7 ERROR - ************** IMDbError ************** thread_id: {thread_index}, {ex}: {titles}")
        print(traceback.format_exc())


def args_search(path: str, files: list[str]):
    print(f"Searching path: {path}, files: {files}")
    file_count: int = len(files)
    if len(files) < THREAD_NB:
        thread_nb: int = int(THREAD_NB / 3)
    else:
        thread_nb: int = THREAD_NB
    files_per_thread: int = int(len(files) / thread_nb)
    remain_files: int = file_count - files_per_thread * thread_nb
    print(f"file_count: {file_count}, thread_nb: {thread_nb}, files_per_thread: {files_per_thread}, remain_files: {remain_files}, file_count / thread_nb: {file_count / thread_nb}")

    threads = []
    i: int = 0
    thread_id: int = 1
    for thread_id in range(1, thread_nb + 1):
        k: int = thread_id * files_per_thread
        print(f"thread_id: {thread_id}, {range(i, k)}, size: {len(files[i:k])}")
        t1 = threading.Thread(
            target=spawn, args=(thread_id, path, files[i:k]), name=thread_id.__str__()
        )
        t1.start()
        threads.append(t1)
        i = k
    print(f"thread_id: {thread_nb + 1}, {range(file_count - remain_files, file_count)}, size: {len(files[file_count - remain_files:file_count])}")
    t1 = threading.Thread(
        target=spawn,
        args=(thread_id + 1, path, files[file_count - remain_files: file_count]),
        name=(thread_nb + 1).__str__(),
    )
    t1.start()
    threads.append(t1)

    for t in threads:
        t.join()
    print("All tasks has been finished")

    if len(props.keys()) > 0:
        save_json(props)
    else:
        save_json({})
    print(f"Threads: {thread_nb}, Time elapsed: {datetime.fromtimestamp(datetime.timestamp(datetime.now()) - start).strftime('%M:%S.%f')} for {len(files)} titles, {datetime.fromtimestamp((datetime.timestamp(datetime.now()) - start) / len(files)).strftime('%M:%S.%f')} per title.")


def path_search(path):
    print(f"Searching into {path}")
    files: list[str] = os.listdir(path)
    print(f"Searching into {files}")
    try:
        files.remove("W")
    except ValueError:
        pass
    try:
        files.remove("W2")
    except ValueError:
        pass
    try:
        files.remove("W3")
    except ValueError:
        pass
    try:
        files.remove("Captures")
    except ValueError:
        pass
    try:
        files.remove("desktop.ini")
    except ValueError:
        pass
    try:
        files.remove("_report.html")
    except ValueError:
        pass

    # print(files)
    for i, file in enumerate(files):
        if (
                IGNORED_FOLDERS.__contains__(file)
                or not file.endswith(SUPPORTED_EXTENSIONS)
                and not os.path.isdir(path + file)
                and file.endswith(".html")
        ):
            files.remove(file)
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
        print(f"Custom args {sys.argv[1:]}")
        args_search(sys.argv[1], sys.argv[2:])
    else:
        print("Default path.")
        # path_search(str(Path.home()) + os.sep + "Videos" + os.sep)
        # path_search(str(Path.home()) + os.sep + "Videos" + os.sep + "W" + os.sep)
        # path_search("D:/Films/W2/")
        # path_search("C:/Users/rivoi/Videos/W/Underworld")

        path_search("C:/Users/ADELE/Videos/")

    sys.exit()
